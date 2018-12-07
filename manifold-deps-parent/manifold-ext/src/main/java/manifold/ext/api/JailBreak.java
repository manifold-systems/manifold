package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gain direct, type-safe access to otherwise inaccessible classes/methods/fields with @{@link JailBreak}.
 * <p/>
 * Annotate the type on a variable, parameter, or new expression with @{@link JailBreak} to avoid the drudgery
 * and vulnerability of Java reflection.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
public @interface JailBreak
{
}
