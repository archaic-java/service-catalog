package work.archaic.service.compiler.v01;

/** A reusable synchronous parser. Implementations document their supported Java language level. */
public interface CompilerAdapter {
  /**
   * Parses exactly the supplied text. A successful empty result means no syntax problems were
   * found, not that semantic compilation would succeed. No source-file I/O, annotation processing,
   * dependency resolution, or class generation is permitted. Calls must be independent and safe
   * to make concurrently; each invocation owns any mutable compiler resources.
   *
   * @throws ParseException if analysis could not complete; never substitute an empty result
   */
  ParseResult parse(SourceSnapshot source) throws ParseException;
}
