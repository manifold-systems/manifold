package manifold.ext.params.rt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE} )
public @interface manifold_params
{
}
