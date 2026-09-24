package work.archaic.service.sqlite.v01;

/** Valid only during the enclosing read or write call. */
public interface Session {
    Statement prepare(String sql) throws SqliteException;

    /** Requests cancellation of running SQL. May be called by another thread while this scope is live. */
    void cancel();
}
