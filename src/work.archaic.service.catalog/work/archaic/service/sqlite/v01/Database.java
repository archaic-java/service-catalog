package work.archaic.service.sqlite.v01;

/** Owns one writer and a bounded set of readers. Close after all operations finish. */
public interface Database extends AutoCloseable {
    <T> T read(Work<T> work) throws SqliteException;

    /** Runs one transaction; commits on return and rolls back on failure. */
    <T> T write(Work<T> work) throws SqliteException;

    @Override void close() throws SqliteException;
}
