package work.archaic.service.logging.v02;

import java.util.concurrent.ExecutorService;

/**
 * A reusable application intent, such as placing an order or handling a user request.
 * Each attempt includes communicating its outcome when that is part of the intent.
 * Each invocation owns a fresh execution identity, trail and timing, even when the same goal is invoked concurrently. Construction does not start work.
 * The execution boundary alone completes an invocation; callers cannot close or finish it.
 */
public interface Goal {
    /**
     * Returns the stable goal name, without per-invocation identifiers.
     * @return the non-blank name
     */
    String name();

    /**
     * Runs work synchronously on the calling thread with a fresh current trail.
     * Normal completion discards the trail. An escaping exception or error
     * causes one failure report to be offered to the configured log before the original
     * throwable is rethrown unchanged. A reporting failure must not replace that throwable;
     * attach it as suppressed when possible. Restore the previous scope on every exit path.
     *
     * <p>Handling a failure response does not fulfill the goal: rethrow the original failure
     * after communicating it. A caught failure followed by normal return is success. If the
     * failure response also throws, preserve the original failure and suppress the response
     * failure when possible. There is no separate outcome or completion API.
     *
     * <p>Version 02 does not support nesting: if this provider already has an active goal on
     * the calling thread, reject the invocation before running the work. Bindings are not
     * inherited by independently started threads. Cancellation has no separate outcome in v02:
     * an escaping interruption is a failure; a normal return is success.
     *
     * @param <X> checked or unchecked failure type
     * @param work action to invoke exactly once
     * @throws X the original failure from work
     * @throws NullPointerException if work is null
     * @throws IllegalStateException if a goal is already active on this provider and thread
     */
    <X extends Throwable> void run(Action<X> work) throws X;

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
    ExecutorService newExecutor();

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

