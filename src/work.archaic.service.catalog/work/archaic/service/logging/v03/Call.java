package work.archaic.service.logging.v03;

/** Synchronous value-returning context work whose checked exception type is preserved. */
@FunctionalInterface
public interface Call<T, E extends Exception> {
    T call() throws E;
}
