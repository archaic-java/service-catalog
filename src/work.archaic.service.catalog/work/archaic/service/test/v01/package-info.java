/**
 * Archaic test framework for simple annotation-based testing.
 *
 * <h2>Overview</h2>
 * <p>This package provides a minimalistic testing framework using annotations and interfaces
 * to define test methods and optional lifecycle hooks.
 *
 * <h2>Usage</h2>
 * <p>To create tests using this framework:
 *
 * <ol>
 * <li>Implement the {@link TestSuite} interface in your test class</li>
 * <li>Annotate test methods with {@link Test @Test}</li>
 * <li>Optionally override {@code setup()} and {@code teardown()} lifecycle methods</li>
 * </ol>
 *
 * <h3>Basic Example</h3>
 * <pre>{@code
 * public class MyTestSuite implements TestSuite {
 *
 *     @Override
 *     public void setup() throws Exception {
 *         // Initialize resources before tests
 *     }
 *
 *     @Test
 *     public void testBasicFunctionality() {
 *         assert true : "This test should pass";
 *     }
 *
 *     @Test(tags = {"slow", "integration"})
 *     public void testWithTags() {
 *         // Test with custom tags
 *         assert 2 + 2 == 4 : "Math should work";
 *     }
 *
 *     @Override
 *     public void teardown() throws Exception {
 *         // Clean up resources after tests
 *     }
 * }
 * }</pre>
 *
 * <h3>Interface Contract</h3>
 * <p>The {@link TestSuite} interface provides:
 * <ul>
 * <li>{@code setup()} - Called once before any test methods in the suite</li>
 * <li>{@code teardown()} - Called once after all test methods complete</li>
 * </ul>
 * <p>Both methods are optional (default implementations are no-ops) and may throw exceptions.
 *
 * <h3>Annotation Features</h3>
 * <p>The {@link Test @Test} annotation supports:
 * <ul>
 * <li>Zero-configuration usage: {@code @Test}</li>
 * <li>Custom tags for categorization: {@code @Test(tags = {"unit", "fast"})}</li>
 * </ul>
 *
 * <h2>Testing Strategy</h2>
 * <p>Use standard Java {@code assert} statements for test assertions:
 * <pre>{@code
 * @Test
 * public void testStringEquality() {
 *     var result = "hello".toUpperCase();
 *     assert "HELLO".equals(result) : "String should be uppercase";
 * }
 * }</pre>
 *
 * @since 1.0
 */
package work.archaic.service.test.v01;