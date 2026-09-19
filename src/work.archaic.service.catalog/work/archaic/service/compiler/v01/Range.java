package work.archaic.service.compiler.v01;

import java.util.Objects;

/** Half-open source range; a zero-width range denotes a point, including end of file. */
public record Range(Position start, Position end) {
  public Range {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (start.compareTo(end) > 0) throw new IllegalArgumentException("Reversed source range");
  }
}
