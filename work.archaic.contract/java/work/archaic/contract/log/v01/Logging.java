package work.archaic.contract.log.v01;


@SuppressWarnings("unused")
public interface Logging {
    void debug(Object... args);
    void info(Object... args);
    void warn(Object... args);
    void error(Object... args);
    }
