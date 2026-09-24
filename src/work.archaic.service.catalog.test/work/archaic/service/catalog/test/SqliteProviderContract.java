package work.archaic.service.catalog.test;

import java.util.ArrayList;
import java.util.ServiceLoader;
import work.archaic.service.sqlite.v01.Sqlite;
import work.archaic.service.test.v02.TestCase;

/** Compatibility entry point for the independently registered SQLite v01 cases. */
public final class SqliteProviderContract {
    private SqliteProviderContract() { }

    public static void main(String[] args) throws Exception {
        boolean assertions = false;
        assert assertions = true : "Tests require -ea";
        if (!assertions) throw new IllegalStateException("Tests require -ea");
        Sqlite provider = ServiceLoader.load(Sqlite.class).findFirst().orElseThrow();
        var cases = new ArrayList<TestCase>();
        SqliteCases.register(provider, cases);
        for (var test : cases) {
            try {
                test.run(message -> System.err.println(test + ": " + message));
            } catch (Exception | Error failure) {
                System.err.println("Failed: " + test);
                throw failure;
            }
        }
        System.out.println("SQLite provider: " + cases.size() + " independent cases passed");
    }
}
