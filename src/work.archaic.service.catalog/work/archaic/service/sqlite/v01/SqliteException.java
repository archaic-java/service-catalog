package work.archaic.service.sqlite.v01;

/** A SQLite error, with its extended result code if supplied by the provider. */
public final class SqliteException extends Exception {
    private final int code;

    public SqliteException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** A provider failure that does not have a SQLite result code. */
    public SqliteException(String message) {
        super(message);
        this.code = 0;
    }

    public SqliteException(String message, Throwable cause) {
        super(message, cause);
        this.code = 0;
    }

    public int code() { return code; }
}
