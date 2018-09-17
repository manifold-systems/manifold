package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import javax.tools.JavaFileManager;
import manifold.util.ReflectUtil;

public class ManPatchLocation implements JavaFileManager.Location
{
  private final GeneratedJavaStubFileObject _fo;

  ManPatchLocation( GeneratedJavaStubFileObject fo )
  {
    _fo = fo;
  }

  @Override
  public String getName()
  {
    return _fo.getName();
  }

  @Override
  public boolean isOutputLocation()
  {
    return false;
  }

  public String inferModuleName( Context ctx )
  {
    Names names = Names.instance( ctx );
    GeneratedJavaStubFileObject fo = _fo;
    String packageName = getPackageName( fo );

    JavacElements elementUtils = JavacElements.instance( ctx );
    for( Object /*ModuleElement*/ ms : (Iterable)ReflectUtil.method( elementUtils, "getAllModuleElements" ).invoke() )
    {
      if( (boolean)ReflectUtil.method( ms, "isUnnamed" ).invoke() )
      {
        continue;
      }

      if( ms.getClass().getSimpleName().equals( "ModuleSymbol" ) )
      {
        //noinspection unchecked
        for( Symbol pkg : (Iterable<Symbol>)ReflectUtil.field( ms, "enclosedPackages" ).get() )
        {
          if( !(pkg instanceof Symbol.PackageSymbol) )
          {
            continue;
          }
          if( pkg.toString().equals( packageName ) )
          {
            //noinspection unchecked,RedundantCast
            Iterable<Symbol> symbolsByName = (Iterable<Symbol>)ReflectUtil.method( ReflectUtil.method( pkg, "members" ).invoke(), "getSymbolsByName", Name.class ).invoke( names.fromString( getClassName( fo ) ) );
            if( symbolsByName.iterator().hasNext() )
            {
              return ReflectUtil.method( ms, "getQualifiedName" ).invoke().toString();
            }
          }
        }
      }
    }
    return null;
  }

  private String getPackageName( GeneratedJavaStubFileObject fo )
  {
    String name = fo.getName();
    int iLast = name.lastIndexOf( '/' );
    if( iLast >= 0 )
    {
      name = name.substring( 0, iLast );
      return name.replace( '/', '.' );
    }
    return "";
  }

  private String getClassName( GeneratedJavaStubFileObject fo )
  {
    String name = fo.getName();
    name = name.substring( name.lastIndexOf( '/' ) + 1, name.lastIndexOf( '.' ) );
    return name;
  }
}
