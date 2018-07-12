package manifold.ext;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import javax.lang.model.type.NoType;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.runtime.protocols.ManClassesUrlConnection;

/**
 * Used at runtime to dynamically proxy a type that structurally (as opposed to nominally)
 * implements an interface annotated with @Structural.
 */
public class StructuralTypeProxyGenerator
{
  private final Class<?> _iface;
  private Class<?> _rootClass;
  private final String _name;
  private Symbol.ClassSymbol _rootClassSymbol;

  private StructuralTypeProxyGenerator( Class<?> iface, Class<?> rootClass, String name )
  {
    _iface = iface;
    _rootClass = rootClass;
    _name = name;
  }

  public static Class makeProxy( Class<?> iface, Class<?> rootClass, final String name )
  {
    StructuralTypeProxyGenerator gen = new StructuralTypeProxyGenerator( iface, rootClass, name );
    String fqnProxy = getNamespace( iface ) + '.' + name;
    ManClassesUrlConnection.putProxySupplier( fqnProxy, () -> gen.generateProxy().toString() );
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

  private StringBuilder generateProxy()
  {
    return new StringBuilder()
      .append( "package " ).append( getNamespace( _iface ) ).append( ";\n" )
      .append( "\n" )
      .append( "public class " ).append( _name ).append( " implements " ).append( _iface.getCanonicalName() ).append( " {\n" )
      .append( "  private final " ).append( _rootClass.getCanonicalName() ).append( " _root;\n" )
      .append( "  \n" )
      .append( "  public " ).append( _name ).append( "(" ).append( _rootClass.getCanonicalName() ).append( " root) {\n" )
      .append( "    _root = root;\n" )
      .append( "  }\n" )
      .append( "  \n" )
      .append( implementIface() )
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

  private String implementIface()
  {
    StringBuilder sb = new StringBuilder();
    // Interface methods
    for( Method mi : _iface.getMethods() )
    {
      genInterfaceMethodDecl( sb, mi, _rootClass );
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

    Class returnType = mi.getReturnType();
    sb.append( "  public " )./*append( getTypeVarList( mi ) ).append( ' ' ).*/append( returnType.getCanonicalName() ).append( ' ' ).append( mi.getName() ).append( "(" );
    Class[] params = mi.getParameterTypes();
    for( int i = 0; i < params.length; i++ )
    {
      Class pi = params[i];
      sb.append( ' ' ).append( pi.getCanonicalName() ).append( " p" ).append( i );
      sb.append( i < params.length - 1 ? ',' : ' ' );
    }
    sb.append( ") {\n" )
      .append( returnType == void.class
               ? "    "
               : "    return " )
      .append( maybeCastReturnType( mi, returnType, rootType ) );
    if( !handleField( sb, mi ) )
    {
      handleMethod( sb, mi, params );
    }
    sb.append( "  }\n" );
  }

  private void handleMethod( StringBuilder sb, Method mi, Class[] params )
  {
    //## todo: maybe we need to explicitly parameterize if the method is generic for some cases?
    sb.append( "_root" ).append( '.' ).append( mi.getName() ).append( "(" );
    for( int i = 0; i < params.length; i++ )
    {
      sb.append( ' ' ).append( "p" ).append( i ).append( i < params.length - 1 ? ',' : ' ' );
    }
    sb.append( ");\n" );
  }

  private boolean handleField( StringBuilder sb, Method method )
  {
    String propertyName = getPropertyNameFromGetter( method );
    if( propertyName != null )
    {
      Field field = findField( propertyName, _rootClass, method.getReturnType(), Variance.Covariant );
      if( field != null )
      {
        sb.append( "_root" ).append( '.' ).append( field.getName() ).append( ";\n" );
        return true;
      }
    }
    else
    {
      propertyName = getPropertyNameFromSetter( method );
      if( propertyName != null )
      {
        Field field = findField( propertyName, _rootClass, method.getParameterTypes()[0], Variance.Contravariant );
        if( field != null )
        {
          sb.append( "_root" ).append( '.' ).append( field.getName() ).append( " = p0;\n" );
          return true;
        }
      }
    }
    return false;
  }

  enum Variance
  {
    Covariant, Contravariant
  }

  private Field findField( String name, Class rootType, Class<?> returnType, Variance variance )
  {
    String nameUpper = Character.toUpperCase( name.charAt( 0 ) ) + (name.length() > 1 ? name.substring( 1 ) : "");
    String nameLower = Character.toLowerCase( name.charAt( 0 ) ) + (name.length() > 1 ? name.substring( 1 ) : "");
    String nameUnder = '_' + nameLower;

    for( Field field : rootType.getFields() )
    {
      String fieldName = field.getName();
      Class<?> toType = variance == Variance.Covariant ? returnType : field.getType();
      Class<?> fromType = variance == Variance.Covariant ? field.getType() : returnType;
      if( (toType.isAssignableFrom( fromType ) ||
           arePrimitiveTypesAssignable( toType, fromType )) &&
          (fieldName.equals( nameUpper ) ||
           fieldName.equals( nameLower ) ||
           fieldName.equals( nameUnder )) )
      {
        return field;
      }
    }
    return null;
  }

  public static boolean arePrimitiveTypesAssignable( Class toType, Class fromType )
  {
    if( toType == null || fromType == null || !toType.isPrimitive() || !fromType.isPrimitive() )
    {
      return false;
    }
    if( toType == fromType )
    {
      return true;
    }

    if( toType == double.class )
    {
      return fromType == float.class ||
             fromType == int.class ||
             fromType == char.class ||
             fromType == short.class ||
             fromType == byte.class;
    }
    if( toType == float.class )
    {
      return fromType == char.class ||
             fromType == short.class ||
             fromType == byte.class;
    }
    if( toType == long.class )
    {
      return fromType == int.class ||
             fromType == char.class ||
             fromType == short.class ||
             fromType == byte.class;
    }
    if( toType == int.class )
    {
      return fromType == short.class ||
             fromType == char.class ||
             fromType == byte.class;
    }
    if( toType == short.class )
    {
      return fromType == byte.class;
    }

    return false;
  }

  private String getPropertyNameFromGetter( Method method )
  {
    Class<?>[] params = method.getParameterTypes();
    if( params.length != 0 )
    {
      return null;
    }
    String name = method.getName();
    String propertyName = null;
    for( String prefix : Arrays.asList( "get", "is" ) )
    {
      if( name.length() > prefix.length() &&
          name.startsWith( prefix ) )
      {
        if( prefix.equals( "is" ) &&
            (!method.getReturnType().equals( boolean.class ) &&
             !method.getReturnType().equals( Boolean.class )) )
        {
          break;
        }

        if( hasPotentialMethod( getRootClassSymbol(), name, method.getParameterCount() ) )
        {
          // try not to let a field match when a method should match
          break;
        }

        propertyName = name.substring( prefix.length() );
        char firstChar = propertyName.charAt( 0 );
        if( firstChar == '_' && propertyName.length() > 1 )
        {
          propertyName = propertyName.substring( 1 );
        }
        else if( Character.isAlphabetic( firstChar ) &&
                 !Character.isUpperCase( firstChar ) )
        {
          propertyName = null;
          break;
        }
      }
    }
    return propertyName;
  }

  private String getPropertyNameFromSetter( Method method )
  {
    if( method.getReturnType() != void.class )
    {
      return null;
    }

    Class<?>[] params = method.getParameterTypes();
    if( params.length != 1 )
    {
      return null;
    }

    String name = method.getName();
    String propertyName = null;
    if( name.length() > "set".length() &&
        name.startsWith( "set" ) )
    {
      if( hasPotentialMethod( getRootClassSymbol(), name, method.getParameterCount() ) )
      {
        // try not to let a field match when a method should match
        return null;
      }

      propertyName = name.substring( "set".length() );
      char firstChar = propertyName.charAt( 0 );
      if( firstChar == '_' && propertyName.length() > 1 )
      {
        propertyName = propertyName.substring( 1 );
      }
      else if( Character.isAlphabetic( firstChar ) &&
               !Character.isUpperCase( firstChar ) )
      {
        propertyName = null;
      }
    }
    return propertyName;
  }

  private boolean hasPotentialMethod( Symbol.ClassSymbol rootClassSymbol, String name, int paramCount )
  {
    if( rootClassSymbol == null || rootClassSymbol instanceof NoType )
    {
      return false;
    }

    for( Symbol member : IDynamicJdk.instance().getMembers( rootClassSymbol, e -> e.flatName().toString().equals( name ) ) )
    {
      Symbol.MethodSymbol methodSym = (Symbol.MethodSymbol)member;
      if( methodSym.getParameters().size() == paramCount )
      {
        return true;
      }
    }
    if( hasPotentialMethod( (Symbol.ClassSymbol)rootClassSymbol.getSuperclass().tsym, name, paramCount ) )
    {
      return true;
    }
    for( Type iface : rootClassSymbol.getInterfaces() )
    {
      if( hasPotentialMethod( (Symbol.ClassSymbol)iface.tsym, name, paramCount ) )
      {
        return true;
      }
    }
    return false;
  }

  private Symbol.ClassSymbol getRootClassSymbol()
  {
    if( _rootClassSymbol == null )
    {
      ClassSymbols classSymbols = ClassSymbols.instance( ManifoldHost.getGlobalModule() );
      BasicJavacTask javacTask = classSymbols.getJavacTask_PlainFileMgr();
      _rootClassSymbol = classSymbols.getClassSymbol( javacTask, _rootClass.getCanonicalName() ).getFirst();
    }
    return _rootClassSymbol;
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
        Parameter[] objParams = objMi.getParameters();
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

  private String maybeCastReturnType( Method mi, Class returnType, Class rootType )
  {
    //## todo:
    return returnType != void.class
           ? "(" + returnType.getCanonicalName() + ")"
           : "";
  }
}
