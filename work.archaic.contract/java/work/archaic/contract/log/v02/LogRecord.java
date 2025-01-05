package work.archaic.contract.log.v02;

import java.util.Optional;

public sealed interface LogRecord {
    record Debug(String message) implements LogRecord {}
    record Info(String message) implements LogRecord {}
    record Warning(String message, Optional<Throwable> throwable) implements LogRecord {
        public Warning(String message) {
            this(message, Optional.empty());
        }
    }
    record Error(String message, Optional<Throwable> throwable) implements LogRecord {
        public Error(String message) {
            this(message, Optional.empty());
        }
    }
}
