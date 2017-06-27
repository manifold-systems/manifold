package manifold.ext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import manifold.ext.api.ExtensionMethod;
import manifold.internal.runtime.protocols.GosuClassesUrlConnection;

/**
 */
public class StructuralTypeProxyGenerator
{
  private String _type;

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
      throw new RuntimeException( e );
    }
  }

  private StringBuilder generateProxy( Class ifaceType, Class type, String name )
  {
    _type = type.getName();
    return new StringBuilder()
      .append( "package " ).append( getNamespace( ifaceType ) ).append( ";\n" )
      .append( "\n" )
      .append( "public class " ).append( name ).append( " implements " ).append( ifaceType.getName() ).append( " {\n" )
      .append( "  private final " ).append( type.getName() ).append( " _root;\n" )
      .append( "  \n" )
      .append( "  public " ).append( name ).append( "(" ).append( type.getName() ).append( " root) {\n" )
      .append( "    _root = root;\n" )
      .append( "  }\n" )
      .append( "  \n" )
      .append( implementIface( ifaceType, type ) )
      .append( "}" );
  }

//  private static boolean isGeneric( GenericDeclaration ifaceType )
//  {
//    return ifaceType.getTypeParameters().length > 0;
//  }

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

  // Note we need to include type variables for generic methods so that the bytecode signatures, which include type vars as params, will be compatible.
  // All other type variable references, including the method's type vars, are erased to their bounding types.
  // e.g.,
  // structure FooBar<T extends CharSequence> {
  //   function foo<E>( e: E ) : T
  // }
  // class MyClass<T extends CharSequence>  { // structurally implemetns FooBar<T>
  //   function foo<E>( e: E ) : T {
  //      return x
  //   }
  // }
  //
  // We generate the followin proxy for MyClass, notice we preserve the E type var for the method:
  //
  // class MyClass_structuralproxy_Foobar implements FooBar {
  //   function foo<E>( e: Object ) : CharSequence {
  //     return _root.foo( e )
  //   }
  // }
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
//    Type returnType = replaceTypeVariableTypeParametersWithBoundingTypes( mi.getGenericReturnType() );
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

//  private static String getTypeVarList( Method fi )
//  {
//    if( isGeneric( fi ) )
//    {
//      StringBuilder vars = new StringBuilder();
//      int i = 0;
//      for( TypeVariable<Method> tv : fi.getTypeParameters() )
//      {
//        vars.append( i++ > 0 ? ',' : '<' );
//        vars.append( getNameWithBounds( tv ) );
//      }
//      if( i > 0 )
//      {
//        return vars.append( '>' ).toString();
//      }
//    }
//    return "";
//  }
//
//  private static String getNameWithBounds( TypeVariable tv )
//  {
//    return tv.getBounds().length == 0 || tv.getBounds()[0] == Object.class
//           ? tv.getName()
//           : (tv.getName() + " extends " + tv.getBounds()[0].getTypeName());
//  }

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
    //return StandardCoercionManager.isStructurallyAssignable_Laxed( mi.getOwnersType(), type, mi, new TypeVarToTypeMap() );
    return true;
  }

  private String maybeCastReturnType( Method mi, Type returnType, Type rootType )
  {
    //## todo:
    return returnType != void.class
           ? "(" + returnType.getTypeName() + ")"
           : "";
  }

//  private static Type replaceTypeVariableTypeParametersWithBoundingTypes( Type type )
//  {
//    return replaceTypeVariableTypeParametersWithBoundingTypes( type, null );
//  }
//  private static Type replaceTypeVariableTypeParametersWithBoundingTypes( Type type, Type enclType )
//  {
//    if( type instanceof TypeVariable )
//    {
////      if( isRecursiveType( (TypeVariableType)type, ((TypeVariableType)type).getBoundingType() ) )
////      {
////        // short-circuit recursive typevar
////        return TypeLord.getPureGenericType( ((TypeVariableType)type).getBoundingType() );
////      }
//
//      if( enclType != null && enclType instanceof ParameterizedType )
//      {
//        TypeVarToTypeMap map = mapTypeByVarName( enclType, enclType );
//        return replaceTypeVariableTypeParametersWithBoundingTypes( getActualType( ((TypeVariableType)type).getBoundingType(), map, true ) );
//      }
//
//      return replaceTypeVariableTypeParametersWithBoundingTypes( ((TypeVariableType)type).getBoundingType(), enclType );
//    }
//
//    if( type.isArray() )
//    {
//      return replaceTypeVariableTypeParametersWithBoundingTypes( type.getComponentType(), enclType ).getArrayType();
//    }
//
//    if( type.isParameterizedType() )
//    {
//      Type[] typeParams = type.getTypeParameters();
//      Type[] concreteParams = new Type[typeParams.length];
//      for( int i = 0; i < typeParams.length; i++ )
//      {
//        concreteParams[i] = replaceTypeVariableTypeParametersWithBoundingTypes( typeParams[i], enclType );
//      }
//      type = type.getParameterizedType( concreteParams );
//    }
//    else
//    {
//      if( type.isGenericType() )
//      {
//        Type[] boundingTypes = new Type[type.getGenericTypeVariables().length];
//        for( int i = 0; i < boundingTypes.length; i++ )
//        {
//          boundingTypes[i] = type.getGenericTypeVariables()[i].getBoundingType();
//
//          if( TypeLord.isRecursiveType( type.getGenericTypeVariables()[i].getTypeVariableDefinition().getType(), boundingTypes[i] ) )
//          {
//            return type;
//          }
//        }
//        for( int i = 0; i < boundingTypes.length; i++ )
//        {
//          boundingTypes[i] = replaceTypeVariableTypeParametersWithBoundingTypes( boundingTypes[i], enclType );
//        }
//        type = type.getParameterizedType( boundingTypes );
//      }
//    }
//    return type;
//  }

}
