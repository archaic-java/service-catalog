package work.archaic.service.sqlite.v01;

@FunctionalInterface
public interface Work<T> {
    T run(Session session) throws SqliteException;
}
