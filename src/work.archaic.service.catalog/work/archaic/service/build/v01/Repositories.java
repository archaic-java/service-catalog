package work.archaic.service.build.v01;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link Repository} annotations.
 * This annotation is automatically used by the Java compiler when multiple
 * {@code @Repository} annotations are applied to a module.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.MODULE)
public @interface Repositories {
  Repository[] value();
}
