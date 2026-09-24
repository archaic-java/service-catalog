package work.archaic.service.sqlite.v01;

/** A single SQL statement. Parameters are 1-based; columns are 0-based. */
public interface Statement extends AutoCloseable {
    Statement bind(int index, long value) throws SqliteException;
    Statement bind(int index, double value) throws SqliteException;
    Statement bind(int index, String value) throws SqliteException;
    Statement bind(int index, byte[] value) throws SqliteException;
    Statement bindNull(int index) throws SqliteException;

    /** Advances the statement; true means a row is available, false means complete. */
    boolean step() throws SqliteException;
    int columns();
    boolean isNull(int column) throws SqliteException;
    long longAt(int column) throws SqliteException;
    double doubleAt(int column) throws SqliteException;
    String stringAt(int column) throws SqliteException;
    byte[] bytesAt(int column) throws SqliteException;

    @Override void close() throws SqliteException;
}
