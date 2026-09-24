package work.archaic.service.sqlite.v01;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/** Owns one writer and a bounded set of readers. Close after all operations finish. */
public interface Database extends AutoCloseable {
    <T, X extends Throwable> T read(Work<T, X> work) throws SqliteException, X;

    /** Runs one transaction; commits on return and rolls back on failure. */
    <T, X extends Throwable> T write(Work<T, X> work) throws SqliteException, X;

    /** Saves a consistent database image to a new file, without replacing an existing file. */
    void backup(Path destination, Duration limit) throws SqliteException, IOException;

    /** Attempts a passive WAL checkpoint; active readers may leave frames pending. */
    Checkpoint checkpoint() throws SqliteException;

    @Override void close() throws SqliteException;
}
