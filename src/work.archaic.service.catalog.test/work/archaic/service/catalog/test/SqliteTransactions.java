package work.archaic.service.catalog.test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import work.archaic.service.sqlite.v01.Database;
import work.archaic.service.sqlite.v01.Sqlite;
import work.archaic.service.sqlite.v01.SqliteException;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestTrail;

/** Transaction cases share only immutable provider configuration; each run owns a database. */
final class TransferChecks {
    private TransferChecks() { }

    static void initialize(Database db) throws SqliteException {
        db.write(session -> {
            try (var statement = session.prepare(
                    "CREATE TABLE accounts (id INTEGER PRIMARY KEY, balance INTEGER NOT NULL CHECK (balance >= 0))")) {
                assert !statement.step() : "Accounts table should be created";
            }
            try (var statement = session.prepare(
                    "CREATE TABLE transfers (id INTEGER PRIMARY KEY, amount INTEGER NOT NULL)")) {
                assert !statement.step() : "Transfers table should be created";
            }
            try (var statement = session.prepare("INSERT INTO accounts VALUES (1, 100), (2, 50)")) {
                assert !statement.step() : "Initial balances should be inserted";
            }
            return null;
        });
    }

    record State(long first, long second, long transfers) { }

    static State state(Database db) throws SqliteException {
        return db.read(session -> {
            long first;
            long second;
            long transfers;
            try (var statement = session.prepare("SELECT id, balance FROM accounts ORDER BY id")) {
                assert statement.step() : "First account must exist";
                assert statement.longAt(0) == 1 : "First account ID must be 1";
                first = statement.longAt(1);
                assert statement.step() : "Second account must exist";
                assert statement.longAt(0) == 2 : "Second account ID must be 2";
                second = statement.longAt(1);
                assert !statement.step() : "Only two accounts must exist";
            }
            try (var statement = session.prepare("SELECT count(*) FROM transfers")) {
                assert statement.step() : "Transfer count should be available";
                transfers = statement.longAt(0);
            }
            return new State(first, second, transfers);
        });
    }

    static void transfer(Database db) throws SqliteException {
        db.write(session -> {
            try (var debit = session.prepare("UPDATE accounts SET balance = balance - 10 WHERE id = 1")) {
                assert !debit.step() : "Debit should complete";
            }
            try (var credit = session.prepare("UPDATE accounts SET balance = balance + 10 WHERE id = 2")) {
                assert !credit.step() : "Credit should complete";
            }
            try (var log = session.prepare("INSERT INTO transfers (amount) VALUES (10)")) {
                assert !log.step() : "Transfer log should complete";
            }
            return null;
        });
    }
}

record TransferRollback(Sqlite provider, int failAfter) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            TransferChecks.initialize(db);
            var failure = new java.io.IOException("abort after statement " + failAfter);
            try {
                db.write(session -> {
                    try (var debit = session.prepare("UPDATE accounts SET balance = balance - 10 WHERE id = 1")) {
                        assert !debit.step() : "Debit should complete";
                    }
                    if (failAfter == 1) throw failure;
                    try (var credit = session.prepare("UPDATE accounts SET balance = balance + 10 WHERE id = 2")) {
                        assert !credit.step() : "Credit should complete";
                    }
                    if (failAfter == 2) throw failure;
                    try (var log = session.prepare("INSERT INTO transfers (amount) VALUES (10)")) {
                        assert !log.step() : "Transfer log should complete";
                    }
                    throw failure;
                });
                throw new AssertionError("Callback failure should escape");
            } catch (java.io.IOException expected) {
                assert expected == failure : "Original callback exception should be preserved";
            }
            var afterFailure = TransferChecks.state(db);
            assert afterFailure.equals(new TransferChecks.State(100, 50, 0))
                    : "Every partial transfer must roll back: " + afterFailure;
            TransferChecks.transfer(db);
            var afterReuse = TransferChecks.state(db);
            assert afterReuse.equals(new TransferChecks.State(90, 60, 1))
                    : "Next writer must transfer exactly once: " + afterReuse;
            assert afterReuse.first() + afterReuse.second() == 150 : "Transfer must conserve total";
        });
    }
}

record TransactionConstraints(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            TransferChecks.initialize(db);
            try {
                db.write(session -> {
                    try (var statement = session.prepare("UPDATE accounts SET balance = -1 WHERE id = 1")) {
                        statement.step();
                    }
                    return null;
                });
                throw new AssertionError("Immediate CHECK violation should fail");
            } catch (SqliteException expected) {
                assert (expected.code() & 0xff) == 19 : "Immediate violation should report SQLITE_CONSTRAINT";
            }
            assert TransferChecks.state(db).equals(new TransferChecks.State(100, 50, 0))
                    : "Immediate failure should retain original rows";
            db.write(session -> {
                try (var statement = session.prepare(
                        "CREATE TABLE references_account (account INTEGER REFERENCES accounts(id) DEFERRABLE INITIALLY DEFERRED)")) {
                    assert !statement.step() : "Deferred foreign-key table should be created";
                }
                return null;
            });
            try {
                db.write(session -> {
                    try (var statement = session.prepare("INSERT INTO references_account VALUES (999)")) {
                        assert !statement.step() : "Deferred violation should reach COMMIT";
                    }
                    return null;
                });
                throw new AssertionError("Deferred foreign-key violation must fail at COMMIT");
            } catch (SqliteException expected) {
                assert (expected.code() & 0xff) == 19 : "COMMIT should report SQLITE_CONSTRAINT";
            }
            assert db.read(session -> {
                try (var statement = session.prepare("SELECT count(*) FROM references_account")) {
                    assert statement.step() : "Reference count should be readable";
                    return statement.longAt(0);
                }
            }) == 0 : "Deferred violation should roll back";
            TransferChecks.transfer(db);
            assert TransferChecks.state(db).equals(new TransferChecks.State(90, 60, 1))
                    : "Writer must remain usable after both constraint failures";
        });
    }
    @Override public String toString() { return "SQLite immediate and deferred constraints at commit"; }
}

record StableSnapshot(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            TransferChecks.initialize(db);
            var established = new CountDownLatch(1);
            var committed = new CountDownLatch(1);
            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var writer = tasks.submit(() -> {
                    try {
                        if (!established.await(3, TimeUnit.SECONDS))
                            throw new AssertionError("Reader did not establish snapshot");
                        TransferChecks.transfer(db);
                    } finally { committed.countDown(); }
                    return true;
                });
                try {
                    db.read(session -> {
                        long before;
                        try (var statement = session.prepare("SELECT balance FROM accounts WHERE id = 1")) {
                            assert statement.step() : "First SELECT should see account";
                            before = statement.longAt(0);
                        }
                        established.countDown();
                        if (!committed.await(3, TimeUnit.SECONDS))
                            throw new AssertionError("Writer did not complete alongside reader");
                        try (var statement = session.prepare("SELECT balance FROM accounts WHERE id = 1")) {
                            assert statement.step() : "Second SELECT should see account";
                            assert statement.longAt(0) == before : "Read scope must retain its first snapshot";
                        }
                        return null;
                    });
                } finally { established.countDown(); }
                assert writer.get(3, TimeUnit.SECONDS) : "Concurrent writer should commit";
            }
            assert TransferChecks.state(db).equals(new TransferChecks.State(90, 60, 1))
                    : "Fresh read must see the committed transfer";
        });
    }
    @Override public String toString() { return "SQLite stable read snapshot across concurrent commit"; }
}

record ReadOnlyReuse(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            TransferChecks.initialize(db);
            try {
                db.read(session -> {
                    try (var statement = session.prepare("UPDATE accounts SET balance = 0 WHERE id = 1")) {
                        statement.step();
                    }
                    return null;
                });
                throw new AssertionError("Read-only scope should reject UPDATE");
            } catch (SqliteException expected) { /* read-only SQLite handle */ }
            assert TransferChecks.state(db).equals(new TransferChecks.State(100, 50, 0))
                    : "Rejected read write must not change rows";
            TransferChecks.transfer(db);
            assert TransferChecks.state(db).equals(new TransferChecks.State(90, 60, 1))
                    : "Next operation should work after read-only rejection";
        });
    }
    @Override public String toString() { return "SQLite read-only rejection and next caller"; }
}
