package work.archaic.service.logging.v01;

/**
 * Provider entry point for goal creation and access to its current execution. Consumers select
 * one provider explicitly, by construction or ServiceLoader. There is no global registry or
 * implicit first-provider selection in this catalog. Providers are safe to share across threads.
 */
public interface GoalProvider {
    /**
     * Creates a reusable goal without starting an execution. The log is supplied explicitly;
     * closing an executor does not close this destination. Providers must not configure the
     * application globally.
     *
     * @param name stable operation name
     * @param log destination for failures
     * @return reusable goal
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if name is blank
     */
    Goal goal(String name, Log log);

    /**
     * Returns the trail active on this provider and calling thread.
     *
     * @return current trail
     * @throws IllegalStateException if no execution is active
     */
    Trail currentTrail();

    /**
     * Contributes an observation without retaining a per-execution trail reference.
     *
     * @param message diagnostic observation
     * @throws IllegalStateException if no execution is active
     * @throws NullPointerException if message is null
     */
    default void note(String message) {
        currentTrail().note(message);
    }
}
