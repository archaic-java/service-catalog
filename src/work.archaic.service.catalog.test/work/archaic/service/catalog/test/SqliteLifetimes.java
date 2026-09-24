package work.archaic.service.catalog.test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import work.archaic.service.sqlite.v01.Session;
import work.archaic.service.sqlite.v01.Sqlite;
import work.archaic.service.sqlite.v01.Statement;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestTrail;

record ExpiredHandles(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            var escapedSession = new AtomicReference<Session>();
            var escapedStatement = new AtomicReference<Statement>();
            db.read(session -> {
                escapedSession.set(session);
                escapedStatement.set(session.prepare("SELECT 17")); // intentionally left open
                return null;
            });
            try {
                escapedSession.get().prepare("SELECT 18");
                throw new AssertionError("Expired session must reject prepare");
            } catch (IllegalStateException expected) { /* expired scope */ }
            try {
                escapedStatement.get().step();
                throw new AssertionError("Forgotten statement must have been closed at scope exit");
            } catch (IllegalStateException expected) { /* closed statement */ }
            assert db.read(session -> {
                try (var statement = session.prepare("SELECT 19")) {
                    assert statement.step() : "Next sole reader should have a row";
                    return statement.longAt(0);
                }
            }) == 19 : "Sole reader must remain usable after handles expire";
        });
    }
    @Override public String toString() { return "SQLite expired session and forgotten statement"; }
}

record ColumnAccessAndClose(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            db.read(session -> {
                var statement = session.prepare("SELECT 42");
                try {
                    try {
                        statement.longAt(0);
                        throw new AssertionError("Column access before first row must fail");
                    } catch (IllegalStateException expected) { /* no current row */ }
                    assert statement.step() : "SELECT should yield one row";
                    try {
                        statement.longAt(-1);
                        throw new AssertionError("Negative column index must fail");
                    } catch (IndexOutOfBoundsException expected) { /* invalid index */ }
                    try {
                        statement.longAt(statement.columns());
                        throw new AssertionError("Column index at count must fail");
                    } catch (IndexOutOfBoundsException expected) { /* invalid index */ }
                    assert statement.longAt(0) == 42 : "Valid column remains accessible";
                    assert !statement.step() : "SELECT should finish";
                    try {
                        statement.longAt(0);
                        throw new AssertionError("Column after completion must fail");
                    } catch (IllegalStateException expected) { /* no current row */ }
                } finally { statement.close(); }
                return null;
            });
            assert db.read(session -> {
                try (var statement = session.prepare("SELECT 43")) {
                    assert statement.step() : "Reader should recover after invalid access";
                    return statement.longAt(0);
                }
            }) == 43 : "Sole reader must still work";
        });
    }
    @Override public String toString() { return "SQLite column positioning and index safety"; }
}

record ValueOwnership(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            db.write(session -> {
                try (var create = session.prepare("CREATE TABLE values_test (n INTEGER, d REAL, t TEXT, b BLOB)")) {
                    assert !create.step() : "Value table should be created";
                }
                return null;
            });
            var input = new byte[] {1, 2, 3};
            String unicode = "Grüße\u0000!\uD83D\uDE80";
            db.write(session -> {
                try (var insert = session.prepare("INSERT INTO values_test VALUES (?, ?, ?, ?)")) {
                    insert.bind(1, Long.MIN_VALUE).bind(2, -1.25).bind(3, unicode).bind(4, input);
                    input[0] = 99;
                    assert !insert.step() : "Bound values should be inserted";
                }
                try (var insert = session.prepare("INSERT INTO values_test VALUES (?, ?, ?, ?)")) {
                    insert.bind(1, Long.MAX_VALUE).bind(2, 0.5).bind(3, "").bind(4, new byte[0]);
                    assert !insert.step() : "Empty values should be inserted";
                }
                try (var insert = session.prepare("INSERT INTO values_test VALUES (?, ?, ?, ?)")) {
                    insert.bindNull(1).bindNull(2).bindNull(3).bindNull(4);
                    assert !insert.step() : "NULL values should be inserted";
                }
                return null;
            });
            record Result(String text, byte[] bytes) { }
            var result = db.read(session -> {
                try (var query = session.prepare("SELECT n, d, t, b FROM values_test ORDER BY rowid")) {
                    assert query.step() : "First row should exist";
                    assert query.longAt(0) == Long.MIN_VALUE : "Minimum signed long should round-trip";
                    assert query.doubleAt(1) == -1.25 : "Negative finite double should round-trip";
                    var copiedText = query.stringAt(2);
                    var copiedBytes = query.bytesAt(3);
                    assert query.step() : "Second row should exist";
                    assert query.longAt(0) == Long.MAX_VALUE : "Maximum signed long should round-trip";
                    assert query.doubleAt(1) == 0.5 : "Positive finite double should round-trip";
                    assert !query.isNull(2) && query.stringAt(2).isEmpty() : "Empty TEXT is not NULL";
                    assert !query.isNull(3) && query.bytesAt(3).length == 0 : "Empty BLOB is not NULL";
                    assert query.step() : "Third row should exist";
                    for (int column = 0; column < 4; column++)
                        assert query.isNull(column) : "NULL column must report NULL: " + column;
                    assert query.stringAt(2) == null : "NULL TEXT must be null";
                    assert query.bytesAt(3) == null : "NULL BLOB must be null";
                    assert !query.step() : "Only three rows should exist";
                    return new Result(copiedText, copiedBytes);
                }
            });
            assert result.text().equals(unicode) : "Copied TEXT survives step, statement close and scope exit";
            assert Arrays.equals(result.bytes(), new byte[] {1, 2, 3})
                    : "Bound and fetched BLOBs own their byte arrays after scope exit";
            assert db.read(session -> {
                try (var query = session.prepare("SELECT b FROM values_test WHERE rowid = 1")) {
                    assert query.step() : "Next sole reader should find original BLOB";
                    return Arrays.equals(query.bytesAt(0), new byte[] {1, 2, 3});
                }
            }) : "Mutating input after bind must not affect stored data";
        });
    }
    @Override public String toString() { return "SQLite scalar boundaries and copied values"; }
}

record ActiveClose(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var holder = tasks.submit(() -> db.read(session -> {
                    try (var statement = session.prepare("SELECT 55")) {
                        assert statement.step() : "Live operation should see row";
                        entered.countDown();
                        if (!release.await(3, TimeUnit.SECONDS))
                            throw new AssertionError("Active reader was not released");
                        return statement.longAt(0);
                    }
                }));
                try {
                    assert entered.await(3, TimeUnit.SECONDS) : "Reader should enter before close";
                    try {
                        db.close();
                        throw new AssertionError("Close during active scope must reject");
                    } catch (IllegalStateException expected) { /* active scope remains live */ }
                } finally { release.countDown(); }
                assert holder.get(3, TimeUnit.SECONDS) == 55 : "Rejected close must not invalidate live scope";
            }
            assert db.read(session -> {
                try (var statement = session.prepare("SELECT 56")) {
                    assert statement.step() : "Next reader should work after rejected close";
                    return statement.longAt(0);
                }
            }) == 56 : "Rejected close must leave database usable";
        }); // fixture closes database after all scopes finish
    }
    @Override public String toString() { return "SQLite active close rejection and subsequent close"; }
}
