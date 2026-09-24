package work.archaic.service.catalog.test;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import work.archaic.service.sqlite.v01.Database;
import work.archaic.service.sqlite.v01.Session;
import work.archaic.service.sqlite.v01.Sqlite;
import work.archaic.service.sqlite.v01.SqliteException;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestTrail;

record LeaseTimeout(Sqlite provider, boolean writer) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            if (writer) TransferChecks.initialize(db);
            var holding = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var holder = tasks.submit(() -> {
                    if (writer) db.write(session -> {
                        holding.countDown();
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Writer was not released");
                        return null;
                    });
                    else db.read(session -> {
                        holding.countDown();
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Reader was not released");
                        return null;
                    });
                    return true;
                });
                try {
                    assert holding.await(3, TimeUnit.SECONDS) : "Holder must acquire sole lease";
                    try {
                        if (writer) db.write(session -> { throw new AssertionError("Timed-out writer ran callback"); });
                        else db.read(session -> { throw new AssertionError("Timed-out reader ran callback"); });
                        throw new AssertionError("Exhausted lease must fail within configured wait");
                    } catch (SqliteException expected) { /* bounded lease admission */ }
                } finally { release.countDown(); }
                assert holder.get(3, TimeUnit.SECONDS) : "Holder must exit cleanly";
            }
            if (writer) {
                TransferChecks.transfer(db);
                assert TransferChecks.state(db).equals(new TransferChecks.State(90, 60, 1))
                        : "Next writer should commit exactly once";
            } else {
                assert db.read(session -> {
                    try (var query = session.prepare("SELECT 71")) {
                        assert query.step() : "Next reader should get its lease";
                        return query.longAt(0);
                    }
                }) == 71 : "Sole reader should be returned after exhaustion";
            }
        });
    }
    @Override public String toString() { return writer ? "SQLite bounded writer lease" : "SQLite bounded reader lease"; }
}

record ExpiredCancellation(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            var old = new AtomicReference<Session>();
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var holder = tasks.submit(() -> db.read(session -> {
                    old.set(session);
                    entered.countDown();
                    if (!release.await(3, TimeUnit.SECONDS)) throw new AssertionError("Scope was not released");
                    return true;
                }));
                try {
                    assert entered.await(3, TimeUnit.SECONDS) : "First scope should be live";
                    old.get().cancel(); // permitted from the case thread while the scope is live
                } finally { release.countDown(); }
                assert holder.get(3, TimeUnit.SECONDS) : "First scope should finish";
            }
            assert db.read(session -> {
                try {
                    old.get().cancel();
                    throw new AssertionError("Expired cancel must not target next borrower");
                } catch (IllegalStateException expected) { /* old scope is closed */ }
                try (var query = session.prepare("SELECT 72")) {
                    assert query.step() : "Next borrower should query after expired cancel";
                    return query.longAt(0);
                }
            }) == 72 : "Sole reader remains usable after cancellation near scope end";
        });
    }
    @Override public String toString() { return "SQLite cancellation at scope boundary and next borrower"; }
}

record ConcurrentTransfers(Sqlite provider) implements TestCase {
    @Override public void run(TestTrail trail) throws Exception {
        SqliteCases.withDatabase(provider, trail, 1, (db, directory) -> {
            TransferChecks.initialize(db);
            db.write(session -> {
                try (var create = session.prepare("CREATE TABLE operation_ids (id INTEGER PRIMARY KEY)")) {
                    assert !create.step() : "Operation ID table should be created";
                }
                return null;
            });
            try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
                for (int id = 1; id <= 12; id++) {
                    int operation = id;
                    futures.add(tasks.submit(() -> {
                        db.write(session -> {
                            try (var debit = session.prepare("UPDATE accounts SET balance = balance - 1 WHERE id = 1")) {
                                assert !debit.step() : "Debit should complete";
                            }
                            try (var credit = session.prepare("UPDATE accounts SET balance = balance + 1 WHERE id = 2")) {
                                assert !credit.step() : "Credit should complete";
                            }
                            try (var insert = session.prepare("INSERT INTO operation_ids VALUES (?)")) {
                                insert.bind(1, operation);
                                assert !insert.step() : "Unique operation should be recorded";
                            }
                            return null;
                        });
                        return true;
                    }));
                }
                for (int index = 0; index < futures.size(); index++) {
                    assert (Boolean) futures.get(index).get(5, TimeUnit.SECONDS)
                            : "Every transfer worker should finish";
                    trail.note("Completed transfer ID " + (index + 1));
                }
            }
            var state = TransferChecks.state(db);
            trail.note("Balances and transfer count: " + state);
            assert state.first() == 88 && state.second() == 62 && state.transfers() == 0
                    : "Twelve one-unit transfers must conserve both actual account balances";
            assert state.first() + state.second() == 150 : "Total balance must be conserved";
            var ids = db.read(session -> {
                var seen = new HashSet<Long>();
                try (var query = session.prepare("SELECT id FROM operation_ids ORDER BY id")) {
                    while (query.step()) seen.add(query.longAt(0));
                }
                return seen;
            });
            assert ids.size() == 12 : "Each operation must commit once";
            for (long id = 1; id <= 12; id++) assert ids.contains(id) : "Missing transfer ID " + id;
        });
    }
    @Override public String toString() { return "SQLite concurrent unique transfers preserve balances"; }
}
