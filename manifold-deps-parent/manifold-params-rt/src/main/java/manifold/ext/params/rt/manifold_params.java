package manifold.ext.params.rt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For internal use only!
 * <p/>
 * Marks a generated "telescoping" method as a subcomponent of a physical optional parameters method ("params method")
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE} )
public @interface manifold_params
{
  //identifies the params method as a list of its parameters
  String value();
}
