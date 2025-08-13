package work.archaic.service.test.v01;

/**
 * Test suite interface for the Archaic test framework.
 *
 * <p>Defines the contract for test suites with optional lifecycle hooks.
 */
public interface TestSuite {
  /** Optional hook invoked once before any @Test in this suite. */
  default void setup() throws Exception {}

  /** Optional hook invoked once after all @Test in this suite. */
  default void teardown() throws Exception {}
}
