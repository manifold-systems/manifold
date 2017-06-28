package manifold.ext;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * !!! For internal use only !!!
 * <p/>
 * This annotation is added to a generated extension method when it is added to the extended class.
 * It serves as a means to easily and efficiently identify an extension method during method call analysis.
 * This annotation is never applied to code on disk.
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface ExtensionMethod
{
  String extensionClass();
}
