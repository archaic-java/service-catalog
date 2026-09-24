package work.archaic.service.sqlite.v01;

import java.nio.file.Path;
import java.time.Duration;

/** Opens a local SQLite database. Providers use the system SQLite library. */
public interface Sqlite {
    /** Wait bounds acquisition of a read connection or the writer. */
    Database open(Path file, int readers, Duration wait) throws SqliteException;
}
