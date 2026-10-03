package work.archaic.service.logging.v03;

/** Service provider that creates independent, single-use logging contexts. */
public interface Log {
    /** Create a context using the provider's documented default configuration. */
    Context context();
    /** Create a context capturing the supplied immutable configuration. */
    Context context(Configuration configuration);
}
