package manifold.ext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import manifold.internal.runtime.protocols.GosuClassesUrlConnection;

/**
 * Used at runtime to dynamically proxy a type that structurally (as opposed to nominally)
 * implements an interface annotated with @Structural.
 */
public class StructuralTypeProxyGenerator
{
  private StructuralTypeProxyGenerator()
  {
  }

  public static Class makeProxy( Class<?> iface, Class<?> rootClass, final String name )
  {
    StructuralTypeProxyGenerator gen = new StructuralTypeProxyGenerator();
    String fqnProxy = getNamespace( iface ) + '.' + name;
    GosuClassesUrlConnection.putProxySupplier( fqnProxy, () -> gen.generateProxy( iface, rootClass, name ).toString() );
    try
    {
      return Class.forName( fqnProxy, false, iface.getClassLoader() );
    }
    catch( ClassNotFoundException e )
    {
      try
      {
        return Class.forName( fqnProxy, false, StructuralTypeProxyGenerator.class.getClassLoader() );
      }
      catch( ClassNotFoundException e1 )
      {
        throw new RuntimeException( e1 );
      }
    }
  }

  private StringBuilder generateProxy( Class ifaceType, Class implType, String name )
  {
    return new StringBuilder()
      .append( "package " ).append( getNamespace( ifaceType ) ).append( ";\n" )
      .append( "\n" )
      .append( "public class " ).append( name ).append( " implements " ).append( ifaceType.getCanonicalName() ).append( " {\n" )
      .append( "  private final " ).append( implType.getCanonicalName() ).append( " _root;\n" )
      .append( "  \n" )
      .append( "  public " ).append( name ).append( "(" ).append( implType.getCanonicalName() ).append( " root) {\n" )
      .append( "    _root = root;\n" )
      .append( "  }\n" )
      .append( "  \n" )
      .append( implementIface( ifaceType, implType ) )
      .append( "}" );
  }

  private static String getNamespace( Class ifaceType )
  {
    String nspace = ifaceType.getPackage().getName();
    if( nspace.startsWith( "java." ) || nspace.startsWith( "javax." ) )
    {
      nspace = "not" + nspace;
    }
    return nspace;
  }

  private String implementIface( Class ifaceType, Class rootType )
  {
    StringBuilder sb = new StringBuilder();
    // Interface methods
    for( Method mi : ifaceType.getMethods() )
    {
      genInterfaceMethodDecl( sb, mi, rootType );
    }

    return sb.toString();
  }

  private void genInterfaceMethodDecl( StringBuilder sb, Method mi, Class rootType )
  {
    if( (mi.isDefault() && !implementsMethod( rootType, mi )) || Modifier.isStatic( mi.getModifiers() ) )
    {
      return;
    }
    if( mi.getAnnotation( ExtensionMethod.class ) != null )
    {
      return;
    }
    if( isObjectMethod( mi ) )
    {
      return;
    }

    Type returnType = mi.getReturnType();
    sb.append( "  public " )./*append( getTypeVarList( mi ) ).append( ' ' ).*/append( returnType.getTypeName() ).append( ' ' ).append( mi.getName() ).append( "(" );
    Type[] params = mi.getParameterTypes();
    for( int i = 0; i < params.length; i++ )
    {
      Type pi = params[i];
      sb.append( ' ' ).append( pi.getTypeName() ).append( " p" ).append( i );
      sb.append( i < params.length - 1 ? ',' : ' ' );
    }
    sb.append( ") {\n" )
      .append( returnType == void.class
               ? "    "
               : "    return " )
      .append( maybeCastReturnType( mi, returnType, rootType ) )
      //## todo: maybe we need to explicitly parameterize if the method is generic for some cases?
      .append( "_root" ).append( "." ).append( mi.getName() ).append( "(" );
    for( int i = 0; i < params.length; i++ )
    {
      sb.append( ' ' ).append( "p" ).append( i ).append( i < params.length - 1 ? ',' : ' ' );
    }
    sb.append( ");\n" )
      .append( "  }\n" );
  }

  public static boolean isObjectMethod( Method mi )
  {
    Class[] paramTypes = null;
    outer:
    for( Method objMi : Object.class.getMethods() )
    {
      if( objMi.getName().equals( mi.getName() ) )
      {
        if( paramTypes == null )
        {
          paramTypes = getParamTypes( mi );
        }
        Parameter[] objParams = mi.getParameters();
        if( objParams.length == paramTypes.length )
        {
          for( int i = 0; i < objParams.length; i++ )
          {
            if( !paramTypes[i].equals( objParams[i].getType() ) )
            {
              continue outer;
            }
          }
        }
        return true;
      }
    }
    return false;
  }

  private static Class[] getParamTypes( Method mi )
  {
    Parameter[] params = mi.getParameters();
    Class[] paramTypes = new Class[params.length];
    for( int i = 0; i < params.length; i++ )
    {
      paramTypes[i] = params[i].getType();
    }
    return paramTypes;
  }

  private boolean implementsMethod( Class type, Method mi )
  {
    //## todo:
    //return isStructurallyAssignable( mi.getOwnersType(), type, mi, new TypeVarToTypeMap() );
    return true;
  }

  private String maybeCastReturnType( Method mi, Type returnType, Type rootType )
  {
    //## todo:
    return returnType != void.class
           ? "(" + returnType.getTypeName() + ")"
           : "";
  }
}
