package work.archaic.service.compiler.v01;

import java.util.List;
import java.util.Objects;

/**
 * Successful analysis tied to its exact input. Notices have no source location and should not
 * be displayed as source diagnostics. Both collections are defensively copied.
 */
public record ParseResult(SourceSnapshot source, List<Diagnostic> diagnostics, List<String> notices) {
  public ParseResult {
    Objects.requireNonNull(source, "source");
    diagnostics = List.copyOf(diagnostics);
    notices = List.copyOf(notices);
  }
}
