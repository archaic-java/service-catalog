package work.archaic.service.sqlite.v01;

@FunctionalInterface
public interface Work<T, X extends Throwable> {
    T run(Session session) throws SqliteException, X;
}
