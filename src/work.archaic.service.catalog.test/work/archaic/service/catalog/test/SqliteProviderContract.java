package work.archaic.service.catalog.test;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import work.archaic.service.sqlite.v01.Sqlite;
import work.archaic.service.sqlite.v01.SqliteException;
import work.archaic.service.sqlite.v01.Session;

/** Executable provider conformance check for SQLite v01. */
public final class SqliteProviderContract {
    public static void main(String[] args) throws Exception {
        boolean assertions = false;
        assert assertions = true : "Tests require -ea";
        if (!assertions) throw new IllegalStateException("Tests require -ea");
        var provider = ServiceLoader.load(Sqlite.class).findFirst().orElseThrow();
        var directory = Files.createTempDirectory("ffm-sqlite-test-");
        var file = directory.resolve("test.db");
        var backup = directory.resolve("backup.db");
        try (var db = provider.open(file, 4, Duration.ofSeconds(2))) {
            assert db.read(session -> {
                try (var mode = session.prepare("PRAGMA journal_mode")) {
                    assert mode.step() : "Journal mode should be reported";
                    return mode.stringAt(0);
                }
            }).equalsIgnoreCase("wal") : "Provider should enable WAL mode";
            assert db.read(session -> {
                try (var foreignKeys = session.prepare("PRAGMA foreign_keys")) {
                    assert foreignKeys.step() : "Foreign key setting should be reported";
                    return foreignKeys.longAt(0);
                }
            }) == 1L : "Every reader should enforce foreign keys";
            db.write(session -> {
                try (var statement = session.prepare("CREATE TABLE entries (id INTEGER PRIMARY KEY, name TEXT NOT NULL, data BLOB)")) {
                    assert !statement.step() : "DDL should finish without rows";
                }
                return null;
            });
            db.write(session -> {
                try (var insert = session.prepare("INSERT INTO entries (name, data) VALUES (?, ?)")) {
                    insert.bind(1, "hello").bind(2, new byte[] {1, 2, 3});
                    assert !insert.step() : "INSERT should finish without rows";
                }
                return null;
            });
            assert db.read(session -> {
                try (var query = session.prepare("SELECT name, data FROM entries WHERE id = ?")) {
                    query.bind(1, 1L);
                    assert query.step() : "Inserted row should be visible";
                    assert query.stringAt(0).equals("hello") : "TEXT must round-trip";
                    assert java.util.Arrays.equals(query.bytesAt(1), new byte[] {1, 2, 3})
                            : "BLOB must round-trip";
                    return query.step() ? -1L : 1L;
                }
            }) == 1L : "A reader should see a committed row";
            try {
                db.write(session -> {
                    try (var insert = session.prepare("INSERT INTO entries (name) VALUES ('rollback')")) {
                        insert.step();
                    }
                    throw new IllegalArgumentException("fail the request");
                });
                throw new AssertionError("Request failure must escape");
            } catch (IllegalArgumentException expected) { /* rollback */ }
            var domainFailure = new java.io.IOException("request failed");
            try {
                db.write(session -> {
                    try (var insert = session.prepare("INSERT INTO entries (name) VALUES ('checked')")) {
                        insert.step();
                    }
                    throw domainFailure;
                });
                throw new AssertionError("Checked failure should escape");
            } catch (java.io.IOException expected) {
                assert expected == domainFailure : "Scope must preserve the original checked failure";
            }
            assert db.read(session -> {
                try (var query = session.prepare("SELECT count(*) FROM entries")) {
                    assert query.step() : "COUNT should return a row";
                    return query.longAt(0);
                }
            }) == 1L : "Failed write should roll back";

            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var writes = new java.util.ArrayList<java.util.concurrent.Future<?>>();
                for (int i = 0; i < 12; i++) {
                    int value = i;
                    writes.add(tasks.submit(() -> {
                        try {
                            db.write(session -> {
                                try (var insert = session.prepare("INSERT INTO entries (name) VALUES (?)")) {
                                    insert.bind(1, "request-" + value);
                                    insert.step();
                                }
                                return null;
                            });
                        } catch (SqliteException e) { throw new RuntimeException(e); }
                    }));
                }
                for (var write : writes) write.get();
            }
            assert db.read(session -> {
                try (var query = session.prepare("SELECT count(*) FROM entries")) {
                    assert query.step() : "COUNT should return a row";
                    return query.longAt(0);
                }
            }) == 13L : "Each virtual-thread write should commit exactly once";

            db.write(session -> {
                try (var insert = session.prepare("INSERT INTO entries (name, data) VALUES (?, ?)")) {
                    insert.bind(1, "Grüße\u0000!â").bind(2, new byte[0]);
                    insert.step();
                }
                return null;
            });
            assert db.read(session -> {
                try (var query = session.prepare("SELECT name, data FROM entries WHERE name = ?")) {
                    query.bind(1, "Grüße\u0000!â");
                    assert query.step() : "UTF-8 TEXT with embedded NUL should round-trip";
                    assert query.stringAt(0).equals("Grüße\u0000!â") : "TEXT length must use bytes";
                    assert query.bytesAt(1).length == 0 : "Empty BLOB is distinct from NULL";
                    return query.isNull(1) ? -1 : 1;
                }
            }) == 1 : "Empty BLOB should remain non-NULL";

            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var writing = new CountDownLatch(1);
                var release = new CountDownLatch(1);
                var pending = tasks.submit(() -> {
                    try {
                        db.write(session -> {
                            try (var insert = session.prepare("INSERT INTO entries (name) VALUES ('pending')")) {
                                insert.step();
                            }
                            writing.countDown();
                            try {
                                if (!release.await(2, TimeUnit.SECONDS))
                                    throw new IllegalStateException("Read did not finish alongside writer");
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("Writer interrupted", e);
                            }
                            return null;
                        });
                    } catch (SqliteException e) { throw new RuntimeException(e); }
                });
                try {
                    assert writing.await(2, TimeUnit.SECONDS) : "Writer should enter its transaction";
                    assert db.read(session -> {
                        try (var query = session.prepare("SELECT count(*) FROM entries")) {
                            assert query.step() : "COUNT should return a row";
                            return query.longAt(0);
                        }
                    }) == 14L : "Reader should continue while writer has uncommitted changes";
                    db.backup(backup, Duration.ofSeconds(2));
                    try (var restored = provider.open(backup, 1, Duration.ofSeconds(2))) {
                        assert restored.read(session -> {
                            try (var query = session.prepare("SELECT count(*) FROM entries")) {
                                assert query.step() : "Backup should be queryable";
                                return query.longAt(0);
                            }
                        }) == 14L : "Backup should contain a committed snapshot during a write";
                    }
                    try {
                        db.backup(backup, Duration.ofSeconds(2));
                        throw new AssertionError("Backup must not replace an existing file");
                    } catch (java.nio.file.FileAlreadyExistsException expected) { /* kept original */ }
                    try {
                        db.read(session -> {
                            try (var update = session.prepare("INSERT INTO entries (name) VALUES ('bad')")) {
                                update.step();
                            }
                            return null;
                        });
                        throw new AssertionError("Read-only scope must reject writes");
                    } catch (SqliteException expected) { /* read-only SQLite handle */ }
                } finally { release.countDown(); }
                pending.get();
            }
            var checkpoint = db.checkpoint();
            assert checkpoint.logFrames() >= 0 : "WAL should report nonnegative frames";
            assert checkpoint.checkpointedFrames() >= 0 : "Checkpoint should report nonnegative frames";
            assert checkpoint.logFrames() >= checkpoint.checkpointedFrames()
                    : "Checkpoint cannot complete more frames than the WAL holds";

            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var running = new AtomicReference<Session>();
                var prepared = new CountDownLatch(1);
                var query = tasks.submit(() -> db.read(session -> {
                    try (var statement = session.prepare(
                            "WITH RECURSIVE seq(n) AS (VALUES(1) UNION ALL SELECT n+1 FROM seq WHERE n<100000000) SELECT sum(n) FROM seq")) {
                        running.set(session);
                        prepared.countDown();
                        statement.step();
                        return statement.longAt(0);
                    }
                }));
                assert prepared.await(2, TimeUnit.SECONDS) : "Long query should be prepared";
                Thread.sleep(25);
                running.get().cancel();
                try {
                    query.get(2, TimeUnit.SECONDS);
                    throw new AssertionError("Cancellation should interrupt the long query");
                } catch (ExecutionException expected) {
                    assert expected.getCause() instanceof SqliteException
                            : "Interrupted SQL should report its SQLite error";
                    assert (((SqliteException) expected.getCause()).code() & 0xff) == 9
                            : "Interrupted SQL should report SQLITE_INTERRUPT";
                }
            }
            assert db.read(session -> {
                try (var statement = session.prepare("SELECT 1")) {
                    assert statement.step() : "Reader should remain usable after cancellation";
                    return statement.longAt(0);
                }
            }) == 1 : "Canceled reader should return cleanly to the pool";
        } finally {
            Files.deleteIfExists(backup.resolveSibling("backup.db-wal"));
            Files.deleteIfExists(backup.resolveSibling("backup.db-shm"));
            Files.deleteIfExists(backup);
            Files.deleteIfExists(file.resolveSibling("test.db-wal"));
            Files.deleteIfExists(file.resolveSibling("test.db-shm"));
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        System.out.println("SQLite provider: transactions, concurrency, backup, checkpoint and cancellation passed");
    }
}
