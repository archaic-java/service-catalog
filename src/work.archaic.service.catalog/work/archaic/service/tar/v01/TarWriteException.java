package work.archaic.service.tar.v01;

public class TarWriteException extends TarException {
    private static final long serialVersionUID = 1L;
    public TarWriteException(String message) {
        super(message);
    }

    public TarWriteException(String message, Throwable cause) {
        super(message, cause);
    }

}
