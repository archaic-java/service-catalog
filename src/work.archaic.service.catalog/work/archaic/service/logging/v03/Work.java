package work.archaic.service.logging.v03;

/** Synchronous work whose checked exception type is preserved by a trail. */
@FunctionalInterface
public interface Work<E extends Exception> {
    void run() throws E;
}
