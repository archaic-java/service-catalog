package work.archaic.service.build.v01;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the clone URL for a required module dependency.
 * This annotation can be repeated to specify repository URLs for multiple dependencies.
 *
 * <p>Usage example:
 * <pre>
 * &#64;Repository(module = "some.module", url = "https://github.com/example/some-module.git")
 * &#64;Repository(module = "other.module", url = "https://github.com/example/other-module.git")
 * module my.module {
 *     requires some.module;
 *     requires other.module;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.MODULE)
@Repeatable(Repositories.class)
public @interface Repository {
  /**
   * The name of the required module.
   */
  String module();

  /**
   * The clone URL for retrieving this module.
   */
  String url();

  /**
   * The git reference (branch or tag) to clone.
   * Defaults to the repository's default branch if not specified.
   */
  String ref() default "";
}
