package work.archaic.service.logging.v03;

import java.util.Objects;

/** Object logging without logger fields. Install one shared provider at startup. */
public interface Logging {
    default String loggingName() { return getClass().getName(); }
    default void immediately(String message) { provider().immediately(loggingName(), message); }
    default void onDebug(String message) { provider().onDebug(loggingName(), message); }
    default void onFailure(String message) { provider().onFailure(loggingName(), message); }

    static <E extends Exception> void trail(Work<E> work) throws E { provider().trail(work); }
    static void failure(String message) { provider().failure(Logging.class.getName(), message); }
    static void debug(boolean enabled) { provider().debug(enabled); }
    static boolean debug() { return provider().debug(); }

    /** Bind once at composition time, normally after explicit ServiceLoader selection. */
    static void install(Log provider) { Binding.install(provider); }

    private static Log provider() {
        Log provider = Binding.provider;
        if (provider == null) throw new IllegalStateException("Install a logging provider first");
        return provider;
    }
}

final class Binding {
    private Binding() {}
    static volatile Log provider;
    static synchronized void install(Log selected) {
        Objects.requireNonNull(selected, "provider");
        if (provider != null) throw new IllegalStateException("Logging provider already installed");
        provider = selected;
    }
}
