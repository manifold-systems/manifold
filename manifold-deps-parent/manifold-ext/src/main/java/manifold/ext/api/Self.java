package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method's return type with @{@link Self} to achieve covariance
 * with respect to subclasses of the method's declaring class.
 * <p/>
 * Note the {@link ElementType#METHOD} target is for <b>internal use only</b>.
 * This is necessary for generated code where even though the code applies the
 * {@link Self} annotation at the method return type position Java 8 misinterprets
 * it as a Method annotation, hence the METHOD target here. The METHOD target type
 * will be removed in a future release.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE, ElementType.METHOD})
public @interface Self
{
}
