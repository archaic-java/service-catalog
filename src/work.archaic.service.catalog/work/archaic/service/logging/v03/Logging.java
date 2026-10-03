package work.archaic.service.logging.v03;

import java.util.function.Supplier;

/** Object logging without logger fields, using the current execution's context. */
public interface Logging {
    default String loggingName() { return getClass().getName(); }
    default void logImmediately(String message) { context().immediately(loggingName(), message); }
    default void logOnDebug(Supplier<String> message) { context().onDebug(loggingName(), message); }
    default void logOnFailure(String message) { context().onFailure(loggingName(), message); }

    /** Access the current context, for example to mark a handled failure with fail(reason). */
    static Context context() {
        Context context = ContextBinding.currentOrNull();
        if (context == null) throw new IllegalStateException("No active logging context");
        return context;
    }
}
