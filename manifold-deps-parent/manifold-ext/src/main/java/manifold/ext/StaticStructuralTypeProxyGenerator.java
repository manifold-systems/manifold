/*
 * Copyright (c) 2021 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.List;
import manifold.api.host.IModule;
import manifold.ext.rt.ExtensionMethod;
import manifold.ext.rt.RuntimeMethods;
import manifold.ext.rt.api.IProxyFactory_gen;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.rt.api.util.Pair;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Solely for IProxyFactory_gen
public class StaticStructuralTypeProxyGenerator
{
  private final String _package;
  private final String _name;
  private final Type _iface;
  private final IModule _module;
  private final ClassSymbol _rootClass;
  private ClassSymbol _rootClassSymbol;

  private StaticStructuralTypeProxyGenerator( String name, Type iface, ClassSymbol rootClass, String extensionPkg, IModule module )
  {
    _package = getNamespace( extensionPkg );
    _name = name;
    _iface = iface;
    _rootClass = rootClass;
    _module = module;
  }

  static Pair<String, String> makeProxy( String name, Type iface, ClassSymbol rootClass, String extensionPkg, IModule module )
  {
    StaticStructuralTypeProxyGenerator gen = new StaticStructuralTypeProxyGenerator( name, iface, rootClass, extensionPkg, module );
    return new Pair<>( gen._package + '.' + gen._name, gen.generateProxy().toString() );
  }

  private StringBuilder generateProxy()
  {
    return new StringBuilder()
      .append( "package " ).append( _package ).append( ";\n\n" )
      .append( "import " ).append( IProxyFactory_gen.class.getTypeName() ).append( ";\n" )
      .append( "\n" )
      .append( "public class " ).append( _name ).append( " implements IProxyFactory_gen<" ).append( _rootClass.getQualifiedName() ).append( ", " ).append( _iface.tsym.getQualifiedName() ).append( ">\n" )
      .append( "{\n" )
      .append( "  @Override\n" )
      .append( "  public " ).append( _iface.tsym.getQualifiedName() ).append( " proxy( " ).append( _rootClass.getQualifiedName() ).append( " root, Class<" ).append( _iface.tsym.getQualifiedName() ).append( "> cls) {\n" )
      .append( "    return new Proxy(root);\n" )
      .append( "  }\n" )
      .append( "  public static class Proxy implements " ).append( eraseParams( _iface ) ).append( " {\n" )
      .append( "    private final " ).append( _rootClass.getQualifiedName() ).append( " _root;\n" )
      .append( "    \n" )
      .append( "    public Proxy(" ).append( _rootClass.getQualifiedName() ).append( " root) {\n" )
      .append( "      _root = root;\n" )
      .append( "    }\n\n" )
      .append( implementIface( _iface, _iface, new HashSet<>(), new StringBuilder() ) )
      .append( "  }\n" )
      .append( "}" );
  }

  // Note, we erase type params for cases like:
  //   public abstract class MyBigDecimalExt implements Comparable<BigDecimal> { ... }
  // where the type parameter is concrete.
  private String eraseParams( Type iface )
  {
    if( !(iface instanceof Type.ClassType) )
    {
      return iface.toString();
    }

    if( !iface.isParameterized() )
    {
      return iface.toString();
    }
    StringBuilder sb = new StringBuilder( iface.tsym.getQualifiedName() ).append( '<' );
    for( Type param: iface.allparams() )
    {
      if( sb.charAt( sb.length()-1 ) != '<' )
      {
        sb.append( ", " );
      }
      sb.append( eraseParams( param ) );
    }
    sb.append( '>' );
    return sb.toString();
  }

  private static String getNamespace( String extensionPkg )
  {
    String nspace = extensionPkg;
    if( nspace.startsWith( "java." ) || nspace.startsWith( "javax." ) )
    {
      nspace = "not" + nspace;
    }
    return nspace;
  }

  private String implementIface( Type originalIface, Type iface, Set<Type> visited, StringBuilder sb )
  {
    if( visited.contains( iface ) )
    {
      return null;
    }
    visited.add( iface );

    for( Type type : ((ClassSymbol)iface.tsym).getInterfaces() )
    {
      implementIface( originalIface, type, visited, sb );
    }

    // Interface methods
    for( Symbol mi : IDynamicJdk.instance().getMembers( (ClassSymbol)iface.tsym ) )
    {
      if( mi instanceof MethodSymbol )
      {
        genInterfaceMethodDecl( sb, originalIface, (MethodSymbol)mi, _rootClass );
      }
    }

    return sb.toString();
  }

  private boolean isSynthetic( MethodSymbol m )
  {
    return (m.flags() & Flags.SYNTHETIC) != 0 ||
      (m.flags() & Flags.BRIDGE) != 0;
  }

  private void genInterfaceMethodDecl( StringBuilder sb, Type iface, MethodSymbol mi, ClassSymbol rootType )
  {
    Types types = Types.instance( JavacPlugin.instance().getContext() );
    mi = (MethodSymbol)mi.asMemberOf( iface, types );

    if( (mi.isDefault() && !implementsMethod( rootType, mi )) ||
      mi.isStatic() || isSynthetic( mi ) )
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

    String returnType = eraseParams( mi.getReturnType() );
    sb.append( "  public " )./*append( getTypeVarList( mi ) ).append( ' ' ).*/append( returnType ).append( ' ' ).append( mi.flatName() ).append( "(" );
    List<VarSymbol> params = mi.getParameters();
    for( int i = 0; i < params.size(); i++ )
    {
      VarSymbol pi = params.get( i );
      sb.append( ' ' ).append( eraseParams( pi.type ) ).append( " p" ).append( i );
      sb.append( i < params.size() - 1 ? ',' : ' ' );
    }
    sb.append( ") {\n" )
      .append( returnType.equals( "void" )
        ? "    "
        : "    return " )
      .append( maybeCastReturnType( mi, returnType, rootType ) );
    if( !mi.getReturnType().isPrimitive() )
    {
      sb.append( RuntimeMethods.class.getTypeName() ).append( ".coerce(" );
    }
    if( !handleField( sb, mi ) )
    {
      handleMethod( sb, mi, params );
    }
    if( !mi.getReturnType().isPrimitive() )
    {
      sb.append( ", " ).append( mi.getReturnType().tsym.toString() ).append( ".class);\n" );
    }
    else
    {
      sb.append( ";\n" );
    }
    sb.append( "  }\n" );
  }

  private void handleMethod( StringBuilder sb, MethodSymbol mi, List<VarSymbol> params )
  {
    //## todo: maybe we need to explicitly parameterize if the method is generic for some cases?
    sb.append( "_root" ).append( '.' ).append( mi.flatName() ).append( "(" );
    for( int i = 0; i < params.size(); i++ )
    {
      sb.append( ' ' ).append( "p" ).append( i ).append( i < params.size() - 1 ? ',' : ' ' );
    }
    sb.append( ")" );
  }

  private boolean handleField( StringBuilder sb, MethodSymbol method )
  {
    String propertyName = getPropertyNameFromGetter( method );
    if( propertyName != null )
    {
      VarSymbol field = findField( propertyName, _rootClass, method.getReturnType(), StaticStructuralTypeProxyGenerator.Variance.Covariant );
      if( field != null )
      {
        sb.append( "_root" ).append( '.' ).append( field.flatName() );
        return true;
      }
    }
    else
    {
      propertyName = getPropertyNameFromSetter( method );
      if( propertyName != null )
      {
        VarSymbol field = findField( propertyName, _rootClass, method.getParameters().get( 0 ).type, StaticStructuralTypeProxyGenerator.Variance.Contravariant );
        if( field != null )
        {
          sb.append( "_root" ).append( '.' ).append( field.flatName() ).append( " = p0;\n" );
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

  private VarSymbol findField( String name, ClassSymbol rootType, Type returnType, Variance variance )
  {
    Types types = Types.instance( JavacPlugin.instance().getContext() );
    rootType = (ClassSymbol)types.erasure( rootType.type ).tsym;
    String nameUpper = Character.toUpperCase( name.charAt( 0 ) ) + (name.length() > 1 ? name.substring( 1 ) : "");
    String nameLower = Character.toLowerCase( name.charAt( 0 ) ) + (name.length() > 1 ? name.substring( 1 ) : "");
    String nameUnder = '_' + nameLower;

    for( Symbol field : IDynamicJdk.instance().getMembers( rootType ) )
    {
      if( !(field instanceof VarSymbol) )
      {
        continue;
      }
      String fieldName = field.flatName().toString();
      Type toType = variance == Variance.Covariant ? returnType : field.type;
      Type fromType = variance == Variance.Covariant ? field.type : returnType;
      if( (types.isAssignable( fromType, toType ) ||
        arePrimitiveTypesAssignable( toType, fromType )) &&
        (fieldName.equals( nameUpper ) ||
          fieldName.equals( nameLower ) ||
          fieldName.equals( nameUnder )) )
      {
        return (VarSymbol)field;
      }
    }
    return null;
  }

  private boolean arePrimitiveTypesAssignable( Type toType, Type fromType )
  {
    if( toType == null || fromType == null || !toType.isPrimitive() || !fromType.isPrimitive() )
    {
      return false;
    }
    if( toType == fromType )
    {
      return true;
    }

    JavacTypes types = JavacTypes.instance( JavacPlugin.instance().getContext() );
    if( toType == types.getPrimitiveType( TypeKind.DOUBLE ) )
    {
      return fromType == types.getPrimitiveType( TypeKind.FLOAT ) ||
        fromType == types.getPrimitiveType( TypeKind.INT ) ||
        fromType == types.getPrimitiveType( TypeKind.CHAR ) ||
        fromType == types.getPrimitiveType( TypeKind.SHORT ) ||
        fromType == types.getPrimitiveType( TypeKind.BYTE );
    }
    if( toType == types.getPrimitiveType( TypeKind.FLOAT ) )
    {
      return fromType == types.getPrimitiveType( TypeKind.CHAR ) ||
        fromType == types.getPrimitiveType( TypeKind.SHORT ) ||
        fromType == types.getPrimitiveType( TypeKind.BYTE );
    }
    if( toType == types.getPrimitiveType( TypeKind.LONG ) )
    {
      return fromType == types.getPrimitiveType( TypeKind.INT ) ||
        fromType == types.getPrimitiveType( TypeKind.CHAR ) ||
        fromType == types.getPrimitiveType( TypeKind.SHORT ) ||
        fromType == types.getPrimitiveType( TypeKind.BYTE );
    }
    if( toType == types.getPrimitiveType( TypeKind.INT ) )
    {
      return fromType == types.getPrimitiveType( TypeKind.SHORT ) ||
        fromType == types.getPrimitiveType( TypeKind.CHAR ) ||
        fromType == types.getPrimitiveType( TypeKind.BYTE );
    }
    if( toType == types.getPrimitiveType( TypeKind.SHORT ) )
    {
      return fromType == types.getPrimitiveType( TypeKind.BYTE );
    }

    return false;
  }

  private String getPropertyNameFromGetter( MethodSymbol method )
  {
    List<VarSymbol> params = method.getParameters();
    if( !params.isEmpty() )
    {
      return null;
    }
    String name = method.flatName().toString();
    String propertyName = null;
    JavacTypes types = JavacTypes.instance( JavacPlugin.instance().getContext() );
    for( String prefix : Arrays.asList( "get", "is" ) )
    {
      if( name.length() > prefix.length() &&
        name.startsWith( prefix ) )
      {
        if( prefix.equals( "is" ) &&
          (!method.getReturnType().equals( types.getPrimitiveType( TypeKind.BOOLEAN ) ) &&
            !method.getReturnType().tsym.getQualifiedName().toString().equals( Boolean.class.getTypeName() )) )
        {
          break;
        }

        if( hasPotentialMethod( getRootClassSymbol(), name, method.getParameters().length() ) )
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

  private String getPropertyNameFromSetter( MethodSymbol method )
  {
    JavacTypes types = JavacTypes.instance( JavacPlugin.instance().getContext() );
    if( !method.getReturnType().equals( types.getNoType( TypeKind.VOID ) ) )
    {
      return null;
    }

    List<VarSymbol> params = method.getParameters();
    if( params.size() != 1 )
    {
      return null;
    }

    String name = method.flatName().toString();
    String propertyName = null;
    if( name.length() > "set".length() &&
      name.startsWith( "set" ) )
    {
      if( hasPotentialMethod( getRootClassSymbol(), name, method.getParameters().size() ) )
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

  private boolean hasPotentialMethod( ClassSymbol rootClassSymbol, String name, int paramCount )
  {
    if( rootClassSymbol == null || rootClassSymbol instanceof NoType )
    {
      return false;
    }

    for( Symbol member : IDynamicJdk.instance().getMembers( rootClassSymbol, e -> e.flatName().toString().equals( name ) ) )
    {
      MethodSymbol methodSym = (MethodSymbol)member;
      if( methodSym.getParameters().size() == paramCount )
      {
        return true;
      }
    }
    if( hasPotentialMethod( (ClassSymbol)rootClassSymbol.getSuperclass().tsym, name, paramCount ) )
    {
      return true;
    }
    for( Type iface : rootClassSymbol.getInterfaces() )
    {
      if( hasPotentialMethod( (ClassSymbol)iface.tsym, name, paramCount ) )
      {
        return true;
      }
    }
    return false;
  }

  private ClassSymbol getRootClassSymbol()
  {
    if( _rootClassSymbol == null )
    {
      ClassSymbols classSymbols = ClassSymbols.instance( _module );
      _rootClassSymbol = classSymbols.getClassSymbol( JavacPlugin.instance().getJavacTask(),
        _rootClass.getQualifiedName().toString() ).getFirst();
    }
    return _rootClassSymbol;
  }

  public static boolean isObjectMethod( MethodSymbol mi )
  {
    List<VarSymbol> paramTypes = null;
    outer:
    for( Method objMi : Object.class.getMethods() )
    {
      if( objMi.getName().equals( mi.flatName().toString() ) )
      {
        if( paramTypes == null )
        {
          paramTypes = mi.getParameters();
        }
        Parameter[] objParams = objMi.getParameters();
        if( objParams.length == paramTypes.size() )
        {
          for( int i = 0; i < objParams.length; i++ )
          {
            if( !paramTypes.get( i ).type.tsym.getQualifiedName().toString().equals( objParams[i].getType().getCanonicalName() ) )
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

  private boolean implementsMethod( ClassSymbol type, MethodSymbol mi )
  {
    //todo: write a version of ReflectUtil#findBestMethod using ClassSymbol and MethodSymbol
    return ghettoImplementsMethod( type, mi );
  }

  private boolean ghettoImplementsMethod( ClassSymbol sym, MethodSymbol mi )
  {
    Types types = Types.instance( JavacPlugin.instance().getContext() );
    for( Symbol m: IDynamicJdk.instance().getMembersByName( sym, mi.getSimpleName() ) )
    {
      if( m instanceof MethodSymbol )
      {
        List<VarSymbol> params = ((MethodSymbol)m).getParameters();
        List<VarSymbol> miParams = mi.getParameters();
        if( params.size() == miParams.size() )
        {
          for( int i = 0, paramsSize = params.size(); i < paramsSize; i++ )
          {
            VarSymbol param = params.get( i );
            if( !types.isAssignable( miParams.get( i ).type, param.type ) )
            {
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  private String maybeCastReturnType( MethodSymbol mi, String returnType, ClassSymbol rootType )
  {
    //## todo:
    return !returnType.equals( "void" )
      ? "(" + returnType + ")"
      : "";
  }
}
