package work.archaic.service.compiler.v01;

/** Analysis failed internally; this is distinct from ordinary syntax diagnostics. */
public final class ParseException extends Exception {
  private static final long serialVersionUID = 1L;

  public ParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
