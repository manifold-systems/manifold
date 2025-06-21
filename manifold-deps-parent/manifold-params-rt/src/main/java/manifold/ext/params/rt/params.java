package manifold.ext.params.rt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For internal use only!
 * <p/>
 * The default values assigned to optional parameters are handled in a separate generated method. This annotation marks
 * this method as such, preserves parameter names, and identifies which are optional.
 * <p/>
 * Additionally, this annotation marks generated "telescoping" method overloads, which delegate to the source method. The
 * method overloads provide binary compatibility and also allow code not using manifold-params to use optional parameters
 * conventionally without naming arguments.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE} )
public @interface params
{
  //identifies the params method as a list of its parameters
  String value();
}
