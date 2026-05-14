package work.archaic.service.test.v01;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark Methods as defined for testing only.
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface TestOnly {
    String reason() default "Exposed for testing purposes";
}
