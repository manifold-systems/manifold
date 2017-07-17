package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to indicate a class is a Manifold Extension class.  Also
 * if an extension method defined in an extension class is intended to be static
 * in the extended class, it must be declared with this annotation, otherwise
 * because there is no 'this' argument there is no place to designate a @This
 * parameter.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Extension
{
}
