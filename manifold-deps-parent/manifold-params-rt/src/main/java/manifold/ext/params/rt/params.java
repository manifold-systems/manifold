package manifold.ext.params.rt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For internal use only!
 * <p/>
 * The default values assigned to optional parameters are maintained in a generated inner class that is the single parameter
 * to a generated method logically mirroring and delegating to the original parameters method. This annotation
 * identifies the generated method as such and stores the parameter names of the original method.
 * <p/>
 * Additionally, this annotation marks generated "telescoping" method overloads, which delegate to the original optional
 * parameters method. The method overloads provide binary compatibility and also allow code not using manifold-params to
 * use optional parameters albeit in a more limited way.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE} )
public @interface params
{
  //identifies the params method as a list of its parameters
  String value();
}
