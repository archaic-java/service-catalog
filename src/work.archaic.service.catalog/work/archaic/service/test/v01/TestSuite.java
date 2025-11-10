package work.archaic.service.test.v01;

/**
 * Test suite interface for the Archaic test framework.
 *
 * <p>Defines the contract for test suites with optional lifecycle hooks.
 */
public interface TestSuite {
  /** Optional hook invoked once per @TestSuite. */
  default void setup() throws Exception {}

  /** Optional hook invoked once after all @Test of this suite have been run. */
  default void teardown() throws Exception {}
}
