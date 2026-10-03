package work.archaic.service.logging.v03;

/** Provider contract for synchronous, caller-thread logging. See docs/logging-v03.md. */
public interface Log {
    /** Publish immediately on the calling thread, inside or outside a trail. */
    void immediately(String source, String message);
    /** Publish immediately only while application-wide debug mode is enabled. */
    void onDebug(String source, String message);
    /** Retain evidence in the active trail; fail explicitly when none is active. */
    void onFailure(String source, String message);
    /** Mark the active trail failed without throwing; preserve the first reason. */
    void failure(String source, String message);
    /** Run synchronously, joining an existing trail; rethrow the original throwable. */
    <E extends Exception> void trail(Work<E> work) throws E;
    /** Application-wide setting on the shared provider instance; initially false. */
    void debug(boolean enabled);
    boolean debug();
}
