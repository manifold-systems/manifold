package manifold.ext.params.rt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * For internal use only!
 * <p/>
 * Identifies methods generated to support binary/backward compatible optional parameter methods.
 * <p/>
 * <i>These methods are synthetic and are purely implementation detail, they should never be called directly by user
 * code.</i>
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.CONSTRUCTOR, ElementType.METHOD} )
public @interface param_overload
{
}
