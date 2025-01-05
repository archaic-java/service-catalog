package work.archaic.contract.log.v02;

public interface Logging {
    /**
     * @param message describing current step on a trail. Will only be logged in case of a later anomaly
     *                or bug.
     */
    void trail(String message);

    /**
     * @param message describing an event outside a trail. Example: "Application initialization finished."
     */
    void event(String message);

    /**
     * @param exception describing circumstances that are not "normal" but do not lead to service degradation.
     *                Example: API XY could not be reached but can be retried
     */
    void anomaly(Exception exception);

    /**
     * @param throwable being an unexpected outcome.
     */
    void bug(Throwable throwable);
}
