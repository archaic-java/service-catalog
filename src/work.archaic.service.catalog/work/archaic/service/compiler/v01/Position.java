package work.archaic.service.compiler.v01;

/** Zero-based line and UTF-16 column in original source text; tabs count as one code unit. */
public record Position(int line, int character) implements Comparable<Position> {
  public Position {
    if (line < 0 || character < 0) throw new IllegalArgumentException("Negative source coordinate");
  }

  @Override
  public int compareTo(Position other) {
    int byLine = Integer.compare(line, other.line);
    return byLine != 0 ? byLine : Integer.compare(character, other.character);
  }
}
