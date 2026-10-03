package work.archaic.service.logging.v03;

import java.util.Objects;
import java.util.function.Supplier;

/** A configured logging execution. Create anywhere; run once, on the calling thread.
 * The final scope implementation binds this context and preserves escaping failures.
 * Providers implement output, evidence collection and completion through the hooks below. */
public abstract class Context {
    private final Configuration configuration;
    private boolean used;
    private boolean active;
    private Thread owner;

    protected Context(Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public final Configuration configuration() { return configuration; }

    /** Execute once synchronously. Scope exit restores the enclosing context before publishing. */
    public final <E extends Exception> void run(Work<E> work) throws E {
        Objects.requireNonNull(work, "work");
        call(() -> { work.run(); return null; });
    }

    /** Execute once and return its value, including null, after completion has published.
     * Shares run's lifecycle, binding, checked exceptions and failure semantics. */
    public final <T, E extends Exception> T call(Call<T, E> work) throws E {
        Objects.requireNonNull(work, "work");
        synchronized (this) {
            if (used) throw new IllegalStateException("Logging context is single-use");
            used = true;
            active = true;
            owner = Thread.currentThread();
        }
        Throwable failure = null;
        try {
            return ContextBinding.where(this, work);
        } catch (Exception | Error cause) {
            failure = cause;
            throw cause;
        } finally {
            synchronized (this) {
                active = false;
                owner = null;
            }
            try {
                finish(failure);
            } catch (RuntimeException | Error outputFailure) {
                if (failure == null) throw outputFailure;
                if (outputFailure != failure) failure.addSuppressed(outputFailure);
            }
        }
    }

    /** Publish immediately in this active context. */
    public abstract void immediately(String source, String message);
    /** Retain evidence for publication if this execution fails. */
    public abstract void onFailure(String source, String message);
    /** Mark this context failed without throwing; preserve the first reason. */
    public abstract void fail(String reason);

    /** Validate arguments without evaluating the supplier when debug is disabled.
     * Enabled messages are evaluated exactly once on the calling thread. */
    public final void onDebug(String source, Supplier<String> message) {
        requireActive();
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(message, "message");
        if (configuration.debug())
            immediately(source, Objects.requireNonNull(message.get(), "debug message"));
    }

    /** Guard provider operations against use before/after execution, on another thread,
     * or through a suspended outer context while a nested context is current. */
    protected final synchronized void requireActive() {
        if (!active || owner != Thread.currentThread() || ContextBinding.currentOrNull() != this)
            throw new IllegalStateException("Logging context is not active on this thread");
    }

    /** Called once after binding restoration. Publish if marked failed or cause is non-null.
     * Release retained evidence even if the output sink throws. */
    protected abstract void finish(Throwable cause);
}
