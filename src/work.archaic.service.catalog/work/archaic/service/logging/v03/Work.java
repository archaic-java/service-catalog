package work.archaic.service.logging.v03;

/** Synchronous context work whose checked exception type is preserved by run. */
@FunctionalInterface
public interface Work<E extends Exception> {
    void run() throws E;
}
