package work.archaic.service.compiler.v01;

import java.util.Objects;

/** A problem in the supplied source; code is empty when the provider has no diagnostic code. */
public record Diagnostic(Range range, Severity severity, String code, String message) {
  public enum Severity { ERROR, WARNING, INFORMATION }

  public Diagnostic {
    Objects.requireNonNull(range, "range");
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
  }
}
