package work.archaic.service.sqlite.v01;

/** Valid only during the enclosing read or write call. */
public interface Session {
    Statement prepare(String sql) throws SqliteException;
}
