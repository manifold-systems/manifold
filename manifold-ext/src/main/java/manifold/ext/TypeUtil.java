package manifold.ext;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import manifold.ext.api.Structural;

/**
 */
public class TypeUtil
{
  public static boolean isStructuralInterface( Symbol sym )
  {
    if( !sym.isInterface() || !sym.hasAnnotations() )
    {
      return false;
    }
    for( Attribute.Compound annotation : sym.getAnnotationMirrors() )
    {
      if( annotation.type.toString().equals( Structural.class.getName() ) )
      {
        return true;
      }
    }
    return false;
  }
}
