package manifold.api.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use this annotation in projected source code to indicate the actual name of a feature as originally specified.
 * For instance, if a specified name is not an acceptable Java identifier name, you'll have to modify the name
 * in your code. It is important to preserve this information for use with IDE tooling.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ActualName
{
  /**
   * @return The actual name as originally specified in the system of record
   */
  String value();
}
