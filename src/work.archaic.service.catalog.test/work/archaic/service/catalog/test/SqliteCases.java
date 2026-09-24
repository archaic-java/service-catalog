package work.archaic.service.catalog.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import work.archaic.service.sqlite.v01.Database;
import work.archaic.service.sqlite.v01.Session;
import work.archaic.service.sqlite.v01.Sqlite;
import work.archaic.service.sqlite.v01.SqliteException;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestTrail;

/** Registers provider-independent SQLite v01 conformance checks. Provider suites supply the service. */
public final class SqliteCases {
    private SqliteCases() { }

    public static void register(Sqlite provider, Collection<TestCase> cases) {
        java.util.Objects.requireNonNull(provider);
        cases.add(new Configuration(provider));
        cases.add(new Transactions(provider));
        cases.add(new ConcurrentWrites(provider));
        cases.add(new Values(provider));
        cases.add(new BackupAndReads(provider));
        cases.add(new CheckpointProgress(provider));
        cases.add(new Cancellation(provider));
        cases.add(new TransferRollback(provider, 1));
        cases.add(new TransferRollback(provider, 2));
        cases.add(new TransferRollback(provider, 3));
        cases.add(new TransactionConstraints(provider));
        cases.add(new StableSnapshot(provider));
        cases.add(new ReadOnlyReuse(provider));
    }

    @FunctionalInterface
    interface Check { void run(Database db, Path directory) throws Exception; }

    static void withDatabase(Sqlite provider, TestTrail trail, Check check) throws Exception {
        withDatabase(provider, trail, 4, check);
    }

    static void withDatabase(Sqlite provider, TestTrail trail, int readers, Check check) throws Exception {
        Path directory = Files.createTempDirectory("sqlite-conformance-");
        trail.note("Database directory: " + directory);
        boolean passed = false;
        try {
            try (var db = provider.open(directory.resolve("test.db"), readers, Duration.ofSeconds(2))) {
                check.run(db, directory);
            }
            passed = true;
        } finally {
            if (passed) {
                try (var paths = Files.walk(directory)) {
                    for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            } else {
                trail.note("Preserved evidence: " + directory);
            }
        }
    }

    static void schema(Database db) throws SqliteException {
        db.write(session -> {
            try (var statement = session.prepare("CREATE TABLE entries (id INTEGER PRIMARY KEY, name TEXT NOT NULL, data BLOB)")) {
                assert !statement.step() : "DDL should finish without rows";
            }
            return null;
        });
    }

    static long count(Database db) throws SqliteException {
        return db.read(session -> {
            try (var query = session.prepare("SELECT count(*) FROM entries")) {
                assert query.step() : "COUNT should return a row";
                return query.longAt(0);
            }
        });
    }

    static void insert(Database db, String name) throws SqliteException {
        db.write(session -> {
            try (var statement = session.prepare("INSERT INTO entries (name) VALUES (?)")) {
                statement.bind(1, name);
                assert !statement.step() : "INSERT should finish without rows";
            }
            return null;
        });
    }
}

record Configuration(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
            assert db.read(session -> {
                try (var mode = session.prepare("PRAGMA journal_mode")) {
                    assert mode.step() : "Journal mode should be reported";
                    return mode.stringAt(0);
                }
            }).equalsIgnoreCase("wal") : "Provider should enable WAL mode";
            assert db.read(session -> {
                try (var keys = session.prepare("PRAGMA foreign_keys")) {
                    assert keys.step() : "Foreign key setting should be reported";
                    return keys.longAt(0);
                }
            }) == 1L : "Reader should enforce foreign keys";
        });
    }
    @Override public String toString() { return "SQLite WAL and foreign keys"; }
}

record Transactions(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
            SqliteCases.schema(db);
            SqliteCases.insert(db, "committed");
            assert SqliteCases.count(db) == 1 : "Committed write should be visible";
            var unchecked = new IllegalArgumentException("fail the request");
            try {
                db.write(session -> {
                    try (var statement = session.prepare("INSERT INTO entries (name) VALUES ('rollback')")) {
                        statement.step();
                    }
                    throw unchecked;
                });
                throw new AssertionError("Unchecked failure must escape");
            } catch (IllegalArgumentException expected) {
                assert expected == unchecked : "Original unchecked failure should escape";
            }
            var checked = new IOException("request failed");
            try {
                db.write(session -> {
                    try (var statement = session.prepare("INSERT INTO entries (name) VALUES ('checked')")) {
                        statement.step();
                    }
                    throw checked;
                });
                throw new AssertionError("Checked failure must escape");
            } catch (IOException expected) {
                assert expected == checked : "Original checked failure should escape";
            }
            assert SqliteCases.count(db) == 1 : "Failed writes should roll back";
        });
    }
    @Override public String toString() { return "SQLite commit and rollback"; }
}

record ConcurrentWrites(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
            SqliteCases.schema(db);
            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var writes = new ArrayList<java.util.concurrent.Future<?>>();
                for (int i = 0; i < 12; i++) {
                    int value = i;
                    writes.add(tasks.submit(() -> {
                        try { SqliteCases.insert(db, "request-" + value); }
                        catch (SqliteException e) { throw new RuntimeException(e); }
                    }));
                }
                for (var write : writes) write.get();
            }
            assert SqliteCases.count(db) == 12 : "Each virtual-thread write should commit exactly once";
        });
    }
    @Override public String toString() { return "SQLite concurrent virtual-thread writes"; }
}

record Values(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
            SqliteCases.schema(db);
            db.write(session -> {
                try (var statement = session.prepare("INSERT INTO entries (name, data) VALUES (?, ?)")) {
                    statement.bind(1, "hello").bind(2, new byte[] {1, 2, 3});
                    assert !statement.step() : "INSERT should finish without rows";
                }
                try (var statement = session.prepare("INSERT INTO entries (name, data) VALUES (?, ?)")) {
                    statement.bind(1, "Grüße\u0000!â").bind(2, new byte[0]);
                    assert !statement.step() : "INSERT should finish without rows";
                }
                return null;
            });
            assert db.read(session -> {
                try (var query = session.prepare("SELECT name, data FROM entries WHERE id = ?")) {
                    query.bind(1, 1L);
                    assert query.step() : "Inserted row should be visible";
                    assert query.stringAt(0).equals("hello") : "TEXT must round-trip";
                    assert Arrays.equals(query.bytesAt(1), new byte[] {1, 2, 3}) : "BLOB must round-trip";
                    return query.step() ? -1L : 1L;
                }
            }) == 1L : "Reader should see a committed row";
            assert db.read(session -> {
                try (var query = session.prepare("SELECT name, data FROM entries WHERE name = ?")) {
                    query.bind(1, "Grüße\u0000!â");
                    assert query.step() : "UTF-8 TEXT with embedded NUL should round-trip";
                    assert query.stringAt(0).equals("Grüße\u0000!â") : "TEXT length must use bytes";
                    assert query.bytesAt(1).length == 0 : "Empty BLOB should have zero length";
                    return query.isNull(1) ? -1 : 1;
                }
            }) == 1 : "Empty BLOB should remain non-NULL";
        });
    }
    @Override public String toString() { return "SQLite TEXT and BLOB values"; }
}

record BackupAndReads(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
            SqliteCases.schema(db);
            SqliteCases.insert(db, "committed");
            var backup = directory.resolve("backup.db");
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
                    assert writing.await(2, TimeUnit.SECONDS) : "Writer should enter transaction";
                    assert SqliteCases.count(db) == 1 : "Reader should continue during uncommitted write";
                    db.backup(backup, Duration.ofSeconds(2));
                    try (var restored = provider.open(backup, 1, Duration.ofSeconds(2))) {
                        assert SqliteCases.count(restored) == 1 : "Backup should contain committed snapshot";
                    }
                    try {
                        db.backup(backup, Duration.ofSeconds(2));
                        throw new AssertionError("Backup must not replace existing file");
                    } catch (java.nio.file.FileAlreadyExistsException expected) { /* kept original */ }
                    try {
                        db.read(session -> {
                            try (var statement = session.prepare("INSERT INTO entries (name) VALUES ('bad')")) {
                                statement.step();
                            }
                            return null;
                        });
                        throw new AssertionError("Read-only scope must reject writes");
                    } catch (SqliteException expected) { /* read-only connection */ }
                } finally { release.countDown(); }
                pending.get();
            }
            assert SqliteCases.count(db) == 2 : "Writer should commit after reader and backup";
        });
    }
    @Override public String toString() { return "SQLite concurrent read, backup and read-only access"; }
}

record CheckpointProgress(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
            SqliteCases.schema(db);
            SqliteCases.insert(db, "committed");
            var checkpoint = db.checkpoint();
            assert checkpoint.logFrames() >= 0 : "WAL should report nonnegative frames";
            assert checkpoint.checkpointedFrames() >= 0 : "Checkpoint should report nonnegative frames";
            assert checkpoint.logFrames() >= checkpoint.checkpointedFrames()
                    : "Checkpoint cannot complete more frames than the WAL holds";
        });
    }
    @Override public String toString() { return "SQLite checkpoint progress"; }
}

record Cancellation(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, (db, directory) -> {
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
                    throw new AssertionError("Cancellation should interrupt long query");
                } catch (ExecutionException expected) {
                    assert expected.getCause() instanceof SqliteException
                            : "Interrupted SQL should report SQLite error";
                    assert (((SqliteException) expected.getCause()).code() & 0xff) == 9
                            : "Interrupted SQL should report SQLITE_INTERRUPT";
                }
            }
            assert db.read(session -> {
                try (var statement = session.prepare("SELECT 1")) {
                    assert statement.step() : "Reader should remain usable after cancellation";
                    return statement.longAt(0);
                }
            }) == 1 : "Canceled reader should return cleanly to pool";
        });
    }
    @Override public String toString() { return "SQLite cancellation and reader reuse"; }
}
