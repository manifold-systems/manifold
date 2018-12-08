package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare a structural interface using this annotation.
 * <p/>
 * See the <a href="http://manifold.systems/docs.html#structural-interfaces">Structural Interfaces</a>
 * documentation for more information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Structural
{
}
