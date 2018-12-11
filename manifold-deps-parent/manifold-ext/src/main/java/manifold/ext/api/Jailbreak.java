package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gain direct, type-safe access to otherwise inaccessible classes/methods/fields with @{@link Jailbreak}.
 * <p/>
 * Annotate the type on a variable, parameter, or new expression with @{@link Jailbreak} to avoid the drudgery
 * and vulnerability of Java reflection.
 * <p/>
 * See the <a href="http://manifold.systems/docs.html#type-safe-reflection">Type-safe Reflection</a> documentation for
 * more information.
 * <p/>
 * See also {@link manifold.ext.extensions.java.lang.Object.ManObjectExt#jailbreak(Object)}
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
public @interface Jailbreak
{
}
