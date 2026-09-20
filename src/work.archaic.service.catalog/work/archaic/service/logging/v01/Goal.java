package work.archaic.service.logging.v01;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * A reusable description of work. Each invocation owns a fresh execution identity, trail and
 * timing, even when the same goal is invoked concurrently. Construction does not start work.
 * The execution boundary alone completes an invocation; callers cannot close or finish it.
 */
public interface Goal {
    /**
     * Returns the stable operation name, without per-invocation identifiers.
     * @return the non-blank name
     */
    String name();

    /**
     * Runs work synchronously on the calling thread with a fresh current trail. Returns its
     * result unchanged. Normal completion discards the trail. An escaping exception or error
     * causes one failure report to be offered to the configured log before the original
     * throwable is rethrown unchanged. A reporting failure must not replace that throwable;
     * attach it as suppressed when possible. Restore the previous scope on every exit path.
     *
     * <p>Version 01 does not support nesting: if this provider already has an active goal on
     * the calling thread, reject the invocation before running the work. Bindings are not
     * inherited by independently started threads. Cancellation has no separate outcome in v01:
     * an escaping interruption is a failure; a normal return is success.
     *
     * @param <T> result type
     * @param <X> checked or unchecked failure type
     * @param work operation to invoke exactly once
     * @return the operation's result, possibly null
     * @throws X the original failure from work
     * @throws NullPointerException if work is null
     * @throws IllegalStateException if a goal is already active on this provider and thread
     */
    <T, X extends Throwable> T call(Operation<T, X> work) throws X;

    /**
     * Runs an action with the same lifecycle and failure semantics as {@link #call}.
     *
     * @param <X> failure type
     * @param work action to invoke
     * @throws X the original failure from work
     */
    default <X extends Throwable> void run(Action<X> work) throws X {
        Objects.requireNonNull(work, "work");
        call(() -> {
            work.run();
            return null;
        });
    }

    /**
     * Creates a new caller-owned executor. Each accepted task that starts runs on its own
     * virtual thread in a fresh execution of this goal. This applies both to execute and
     * submit: submitted failures must be reported before being captured by their Future.
     * Rejected tasks and tasks cancelled before starting create no execution or report.
     *
     * <p>The executor follows the standard ExecutorService lifecycle; closing it waits for
     * termination. Closing one executor does not close this reusable goal or other executors.
     * There is no inherited parent goal. The executor can observe only failures escaping the
     * actual task, not exceptions caught inside it or failures encoded in return values.
     *
     * @return a new executor whose shutdown belongs to the caller
     */
    ExecutorService executor();

    /**
     * An operation that preserves its declared failure type.
     * @param <T> result type
     * @param <X> failure type
     */
    @FunctionalInterface
    interface Operation<T, X extends Throwable> {
        /**
         * Performs the operation.
         * @return the result
         * @throws X if the operation fails
         */
        T call() throws X;
    }

    /**
     * An action that preserves its declared failure type.
     * @param <X> failure type
     */
    @FunctionalInterface
    interface Action<X extends Throwable> {
        /**
         * Performs the action.
         * @throws X if the action fails
         */
        void run() throws X;
    }
}
