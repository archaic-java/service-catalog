package work.archaic.service.sqlite.v01;

/** Progress reported by a passive WAL checkpoint, measured in WAL frames. */
public record Checkpoint(int logFrames, int checkpointedFrames) {
    public boolean complete() { return logFrames == checkpointedFrames; }
}
