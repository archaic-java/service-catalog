package work.archaic.service.compiler.v01;

import java.net.URI;
import java.util.Objects;

/** Exact in-memory source, its original absolute identity, and a Java basename. */
public record SourceSnapshot(URI uri, String fileName, String text) {
  public SourceSnapshot {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(text, "text");
    if (!uri.isAbsolute()) throw new IllegalArgumentException("Source URI must be absolute");
    if (!fileName.endsWith(".java") || fileName.length() <= 5
        || fileName.contains("/") || fileName.contains("\\") || fileName.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("Expected a Java basename, including module-info.java");
    }
  }
}
