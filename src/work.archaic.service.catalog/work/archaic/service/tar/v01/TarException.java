package work.archaic.service.tar.v01;

public class TarException extends Exception {
  private static final long serialVersionUID = 1L;

  public TarException(String message) {
    super(message);
  }

  public TarException(String message, Throwable cause) {
    super(message, cause);
  }
}
