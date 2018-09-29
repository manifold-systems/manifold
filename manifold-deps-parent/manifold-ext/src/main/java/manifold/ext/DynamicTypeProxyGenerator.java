package manifold.ext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import manifold.api.type.ActualName;
import manifold.ext.api.AbstractDynamicTypeProxy;
import manifold.internal.runtime.protocols.ManClassesUrlConnection;

/**
 * Used at runtime to dynamically proxy a type that dynamically implements a structural interface via ICallHandler
 * e.g., an Extension Method for ICallHandler#call on Map could delegate get/set accessor calls to the map's
 * key/value pairs and delegate method calls to key/value pairs involving functional interface values.
 * <p/>
 * The basic idea is to enable a manifold extension to dynamically dispatch calls to a structural interface via ICallHandler.
 */
public class DynamicTypeProxyGenerator
{
  private DynamicTypeProxyGenerator()
  {
  }

  public static Class makeProxy( Class<?> iface, Class<?> rootClass, final String name )
  {
    DynamicTypeProxyGenerator gen = new DynamicTypeProxyGenerator();
    String fqnProxy = getNamespace( iface ) + '.' + name;
    ManClassesUrlConnection.putProxySupplier( fqnProxy, () -> gen.generateProxy( iface, rootClass, name ).toString() );
    try
    {
      return Class.forName( fqnProxy, false, iface.getClassLoader() );
    }
    catch( ClassNotFoundException e )
    {
      try
      {
        return Class.forName( fqnProxy, false, DynamicTypeProxyGenerator.class.getClassLoader() );
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
      .append( "public class " ).append( name ).append( " extends " ).append( AbstractDynamicTypeProxy.class.getName() ).append( ' ' ).append( " implements " ).append( ifaceType.getCanonicalName() ).append( " {\n" )
      .append( "  private final " ).append( implType.getCanonicalName() ).append( " _root;\n" )
      .append( "  \n" )
      .append( "  public " ).append( name ).append( "(" ).append( implType.getCanonicalName() ).append( " root) {\n" )
      .append( "    super(root);\n" )
      .append( "    _root = root;\n" )
      .append( "  }\n" )
      .append( "  \n" )
      .append( implementIface( ifaceType ) )
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

  private String implementIface( Class ifaceType )
  {
    StringBuilder sb = new StringBuilder();
    // Interface methods
    for( Method mi : ifaceType.getMethods() )
    {
      genInterfaceMethodDecl( sb, mi, ifaceType );
    }

    return sb.toString();
  }

  private void genInterfaceMethodDecl( StringBuilder sb, Method mi, Class ifaceType )
  {
    if( mi.isDefault() || Modifier.isStatic( mi.getModifiers() ) )
    {
      return;
    }
    if( mi.getAnnotation( ExtensionMethod.class ) != null )
    {
      return;
    }
    if( StructuralTypeProxyGenerator.isObjectMethod( mi ) )
    {
      return;
    }

    ActualName anno = mi.getAnnotation( ActualName.class );
    String actualName = anno == null ? "null" : "\""+anno.value()+"\"";
    Class returnType = mi.getReturnType();
    sb.append( "  public " )./*append( getTypeVarList( mi ) ).append( ' ' ).*/append( returnType.getCanonicalName() ).append( ' ' ).append( mi.getName() ).append( "(" );
    Class[] params = mi.getParameterTypes();
    for( int i = 0; i < params.length; i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      Class pi = params[i];
      sb.append( pi.getCanonicalName() ).append( " p" ).append( i );
    }
    sb.append( ") {\n" )
      .append( returnType == void.class
               ? "    "
               : "    return " )
      .append( maybeCastReturnType( returnType ) );
      //## todo: maybe we need to explicitly parameterize if the method is generic for some cases?
    if( returnType != void.class )
    {
      sb.append( RuntimeMethods.class.getTypeName() ).append( ".coerce(" );
    }
    handleCall( sb, mi, ifaceType, actualName, params );
    if( returnType != void.class )
    {
      sb.append( ", " ).append( mi.getReturnType().getCanonicalName() ).append( ".class);\n" );
    }
    else
    {
      sb.append( ";\n" );
    }
    sb.append( "  }\n" );
  }

  private void handleCall( StringBuilder sb, Method mi, Class ifaceType, String actualName, Class[] params )
  {
    sb.append( "_root" ).append( ".call(" ).append( ifaceType.getCanonicalName() ).append( ".class, \"" ).append( mi.getName() ).append( "\", " ).append( actualName ).append( ", " ).append( mi.getReturnType().getCanonicalName() ).append( ".class, " ).append( "new Class[] {" );
    Class<?>[] parameterTypes = mi.getParameterTypes();
    for( int i = 0; i < parameterTypes.length; i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      Class paramType = parameterTypes[i];
      sb.append( paramType.getCanonicalName() ).append( ".class" );
    }
    sb.append( "}, " );
    sb.append( "new Object[] {" );
    for( int i = 0; i < params.length; i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( " p" ).append( i );
    }
    sb.append( "})" );
  }

  private String maybeCastReturnType( Class returnType )
  {
    return returnType != void.class
           ? "(" + returnType.getCanonicalName() + ")"
           : "";
  }
}
