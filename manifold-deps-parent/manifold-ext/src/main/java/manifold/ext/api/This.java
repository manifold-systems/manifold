package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the receiver of an instance extension method.  The receiver's
 * type must match the extended type declared in the package statement of the
 * extension class.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface This
{
}
