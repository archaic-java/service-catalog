package work.archaic.service.tar.v01;

public class TarReadException extends TarException {
  private static final long serialVersionUID = 1L;
    public TarReadException(String message) {
        super(message);
    }

    public TarReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
