package manifold.ext;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import manifold.ext.api.Structural;
import manifold.internal.javac.TypeProcessor;

/**
 */
public class TypeUtil
{
  public static boolean isStructuralInterface( TypeProcessor tp, Symbol sym )
  {
    if( !sym.isInterface() || !sym.hasAnnotations() )
    {
      return false;
    }

    // use the raw type
    Type type = tp.getTypes().erasure( sym.type );
    sym = type.tsym;

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
