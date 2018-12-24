/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import manifold.api.gen.SrcAnnotated;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcType;
import manifold.api.host.IModule;
import manifold.util.ReflectUtil;


import static com.sun.tools.javac.code.TypeTag.CLASS;
import static manifold.util.JreUtil.isJava8;

/**
 */
public class SrcClassUtil
{
  private static final SrcClassUtil INSTANCE = new SrcClassUtil();

  private SrcClassUtil()
  {
  }

  public static SrcClassUtil instance()
  {
    return INSTANCE;
  }

  public SrcClass makeStub( IModule module, String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask )
  {
    return makeStub( module, fqn, classSymbol, compilationUnit, javacTask, true );
  }

  public SrcClass makeStub( IModule module, String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, boolean withMembers )
  {
    return makeSrcClass( module, fqn, classSymbol, compilationUnit, javacTask, withMembers );
  }

  private SrcClass makeSrcClass( IModule module, String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, boolean withMembers )
  {
    SrcClass srcClass = new SrcClass( fqn, SrcClass.Kind.from( classSymbol.getKind() ) )
      .modifiers( classSymbol.getModifiers() );
    if( classSymbol.getEnclosingElement() instanceof Symbol.PackageSymbol && compilationUnit != null )
    {
      for( ImportTree imp: compilationUnit.getImports() )
      {
        if( imp.isStatic() )
        {
          srcClass.addStaticImport( imp.getQualifiedIdentifier().toString() );
        }
        else
        {
          srcClass.addImport( imp.getQualifiedIdentifier().toString() );
        }
      }
    }
    addAnnotations( srcClass, classSymbol );
    for( Symbol.TypeVariableSymbol typeVar: classSymbol.getTypeParameters() )
    {
      srcClass.addTypeVar( makeTypeVarType( typeVar ) );
    }
    Type superclass = classSymbol.getSuperclass();
    if( !(superclass instanceof NoType) )
    {
      srcClass.superClass( makeNestedType( superclass ) );
    }
    for( Type iface: classSymbol.getInterfaces() )
    {
      srcClass.addInterface( makeNestedType( iface ) );
    }
    if( withMembers )
    {
      for( Symbol sym: classSymbol.getEnclosedElements() )
      {
// include private members because:
// 1. @Jailbreak can expose private members
// 2. Compiler error messages are better when referencing an inaccessible method vs. a non-existent one
//        long modifiers = SrcAnnotated.modifiersFrom( sym.getModifiers() );
//        if( Modifier.isPrivate( (int)modifiers ) )
//        {
//          continue;
//        }

        if( sym instanceof Symbol.ClassSymbol )
        {
          addInnerClass( module, srcClass, sym, javacTask );
        }
        else if( sym instanceof Symbol.VarSymbol )
        {
          addField( srcClass, sym );
        }
        else if( sym instanceof Symbol.MethodSymbol )
        {
          if( !isEnumMethod( sym ) )
          {
            addMethod( module, srcClass, (Symbol.MethodSymbol)sym, javacTask );
          }
        }
      }
    }
    return srcClass;
  }

  private boolean isEnumMethod( Symbol sym )
  {
    return sym.getEnclosingElement().isEnum() &&
           (sym.toString().equals( "values()" ) || sym.toString().equals( "valueOf(java.lang.String)" ));
  }

  private SrcType makeNestedType( Type type )
  {
    String fqn = type.toString();
    Type enclosingType = type.getEnclosingType();
    SrcType srcType;
    if( enclosingType != null && !(enclosingType instanceof NoType) && fqn.length() > enclosingType.toString().length() )
    {
      String simpleName = fqn.substring( enclosingType.toString().length() + 1 );
      srcType = new SrcType( simpleName );
      srcType.setEnclosingType( makeNestedType( enclosingType ) );
    }
    else
    {
      srcType = new SrcType( fqn );
    }
    return srcType;
  }

  private void addInnerClass( IModule module, SrcClass srcClass, Symbol sym, BasicJavacTask javacTask )
  {
    SrcClass innerClass = makeSrcClass( module, sym.getQualifiedName().toString(), (Symbol.ClassSymbol)sym, null, javacTask, true );
    srcClass.addInnerClass( innerClass );
  }

  private void addField( SrcClass srcClass, Symbol sym )
  {
    Symbol.VarSymbol field = (Symbol.VarSymbol)sym;
    SrcField srcField = new SrcField( field.name.toString(), makeSrcType( field.type, sym, TargetType.FIELD, -1 ) );
    if( sym.isEnum() )
    {
      srcField.enumConst();
      srcClass.addEnumConst( srcField );
    }
    else
    {
      srcField.modifiers( field.getModifiers() );
      if( Modifier.isFinal( (int)srcField.getModifiers() ) )
      {
        srcField.initializer( new SrcRawExpression( getValueForType( sym.type ) ) );
      }
      srcClass.addField( srcField );
    }
  }

  private void addMethod( IModule module, SrcClass srcClass, Symbol.MethodSymbol method, BasicJavacTask javacTask )
  {
    String name = method.flatName().toString();
    SrcMethod srcMethod = new SrcMethod( srcClass, name.equals( "<init>" ) );
    addAnnotations( srcMethod, method );
    srcMethod.modifiers( method.getModifiers() );
    if( (method.flags() & Flags.VARARGS) != 0 )
    {
      srcMethod.modifiers( srcMethod.getModifiers() | 0x00000080 ); // Modifier.VARARGS
    }
    if( name.equals( "<clinit>" ) )
    {
      return;
    }
    if( !srcMethod.isConstructor() )
    {
      srcMethod.name( name );
      srcMethod.returns( makeSrcType( method.getReturnType(), method, TargetType.METHOD_RETURN, -1 ) );
    }
    for( Symbol.TypeVariableSymbol typeVar: method.getTypeParameters() )
    {
      srcMethod.addTypeVar( makeTypeVarType( typeVar ) );
    }
    List<Symbol.VarSymbol> parameters = method.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      Symbol.VarSymbol param = parameters.get( i );
      SrcParameter srcParam = new SrcParameter( param.flatName().toString(), makeSrcType( param.type, method, TargetType.METHOD_FORMAL_PARAMETER, i ) );
      srcMethod.addParam( srcParam );
      addAnnotations( srcParam, param );
    }
    List<Type> thrownTypes = method.getThrownTypes();
    for( int i = 0; i < thrownTypes.size(); i++ )
    {
      Type throwType = thrownTypes.get( i );
      srcMethod.addThrowType( makeSrcType( throwType, method, TargetType.THROWS, i ) );
    }
    String bodyStmt;
    if( srcMethod.isConstructor() && !srcClass.isEnum() )
    {
      // Note we can't just throw an exception for the ctor body, the compiler will
      // still complain about the missing super() call if the super class does not have
      // an accessible default ctor. To appease the compiler we generate a super(...)
      // call to the first accessible constructor we can find in the super class.
      bodyStmt = genSuperCtorCall( module, srcClass, javacTask );
    }
    else
    {
      bodyStmt = "throw new RuntimeException();";
    }
    srcMethod.body( new SrcStatementBlock()
      .addStatement(
        new SrcRawStatement()
          .rawText( bodyStmt ) ) );
    srcClass.addMethod( srcMethod );
  }

  private SrcType makeSrcType( Type type, Symbol symbol, TargetType targetType, int index )
  {
    SrcType srcType;
    List<Attribute.TypeCompound> annotationMirrors = type.getAnnotationMirrors();
    if( annotationMirrors != null && !annotationMirrors.isEmpty() )
    {
      String unannotatedType = isJava8()
                               ? ReflectUtil.method( type, "unannotatedType" ).invoke().toString()
                               : ReflectUtil.method( type, "cloneWithMetadata", ReflectUtil.type( "com.sun.tools.javac.code.TypeMetadata" ) )
                                 .invoke( ReflectUtil.field( "com.sun.tools.javac.code.TypeMetadata", "EMPTY" ).getStatic() ).toString();
      srcType = new SrcType( unannotatedType );
    }
    else
    {
      srcType = new SrcType( typeNoAnnotations( type ) );
    }
    SymbolMetadata metadata = symbol.getMetadata();
    if( metadata == null || metadata.isTypesEmpty() )
    {
      return srcType;
    }
    List<Attribute.TypeCompound> typeAttributes = metadata.getTypeAttributes();
    if( typeAttributes.isEmpty() )
    {
      return null;
    }

    java.util.List<Attribute.TypeCompound> targetedTypeAttrs = typeAttributes.stream()
      .filter( attr -> attr.getPosition().type == targetType && isTargetIndex( targetType, attr, index ) )
      .collect( Collectors.toList() );

    annotateType( srcType, targetedTypeAttrs );
    return srcType;
  }

  private String typeNoAnnotations( Type type )
  {
    if( isJava8() )
    {
      return type.toString();
    }

    StringBuilder sb = new StringBuilder();
    if( type instanceof Type.ClassType )
    {
      if( type.getEnclosingType().hasTag( CLASS ) &&
          ReflectUtil.field( type.tsym.owner, "kind" ).get() == ReflectUtil.field( "com.sun.tools.javac.code.Kinds$Kind", "TYP" ).getStatic() )
      {
        sb.append( typeNoAnnotations( type.getEnclosingType() ) );
        sb.append( "." );
        sb.append( ReflectUtil.method( type, "className", Symbol.class, boolean.class ).invoke( type.tsym, false ) );
      }
      else
      {
        sb.append( ReflectUtil.method( type, "className", Symbol.class, boolean.class ).invoke( type.tsym, true ) );
      }

      List<Type> typeArgs = type.getTypeArguments();
      if( typeArgs.nonEmpty() )
      {
        sb.append( '<' );
        for( int i = 0; i < typeArgs.size(); i++ )
        {
          if( i > 0 )
          {
            sb.append( ", " );
          }
          Type typeArg = typeArgs.get( i );
          sb.append( typeNoAnnotations( typeArg ) );
        }
        sb.append( ">" );
      }
    }
    else if( type instanceof Type.ArrayType )
    {
      sb.append( typeNoAnnotations( ((Type.ArrayType)type).getComponentType() ) ).append( "[]" );
    }
    else if( type instanceof Type.WildcardType )
    {
      Type.WildcardType wildcardType = (Type.WildcardType)type;
      BoundKind kind = wildcardType.kind;
      sb.append( kind.toString() );
      if( kind != BoundKind.UNBOUND )
      {
        sb.append( typeNoAnnotations( wildcardType.type ) );
      }
    }
    else
    {
      sb.append( type.toString() );
    }
    return sb.toString();
  }

  private boolean isTargetIndex( TargetType targetType, Attribute.TypeCompound attr, int index )
  {
    switch( targetType )
    {
      case METHOD_FORMAL_PARAMETER:
        return attr.getPosition().parameter_index == index;

      case THROWS:
        return attr.getPosition().type_index == index;

      default:
        return index < 0;
    }
  }

  private void annotateType( SrcType srcType, java.util.List<Attribute.TypeCompound> attributes )
  {
    if( attributes.isEmpty() )
    {
      return;
    }

    for( Attribute.TypeCompound attr: attributes )
    {
      if( srcType.isArray() )
      {
        SrcType componentType = srcType.getComponentType();
        addAnnotation( componentType, attr );
      }
      else if( isClassType( srcType ) )
      {
        TypeAnnotationPosition attrPos = attr.getPosition();
        List<TypeAnnotationPosition.TypePathEntry> attrLocation = attrPos == null ? List.nil() : attrPos.location;
        if( attrLocation.isEmpty() )
        {
          addAnnotation( srcType, attr );
        }
        else
        {
          java.util.List<SrcType> typeArguments = srcType.getTypeParams();
          for( int i = 0; i < typeArguments.size(); i++ )
          {
            SrcType typeParam = typeArguments.get( i );
            if( i == attrLocation.get( 0 ).arg )
            {
              List<TypeAnnotationPosition.TypePathEntry> attrLocationCopy = List.from( attrLocation.subList( 1, attrLocation.size() ) );
              TypeAnnotationPosition posCopy = getTypeAnnotationPosition( attrLocationCopy );
              annotateType( typeParam, Collections.singletonList( new Attribute.TypeCompound( attr.type, attr.values, posCopy ) ) );
            }
          }
        }
      }
      else if( "?".equals( srcType.getName() ) && !srcType.getBounds().isEmpty() )
      {
        TypeAnnotationPosition attrPos = attr.getPosition();
        List<TypeAnnotationPosition.TypePathEntry> attrLocation = attrPos.location;
        List<TypeAnnotationPosition.TypePathEntry> attrLocationCopy = null;
        if( !attrLocation.isEmpty() )
        {
          attrLocationCopy = List.from( attrLocation.subList( 1, attrLocation.size() ) );
        }

        if( attrLocationCopy == null || attrLocationCopy.isEmpty() )
        {
          addAnnotation( srcType, attr );
        }
        else
        {
          //noinspection ConstantConditions
          TypeAnnotationPosition posCopy = getTypeAnnotationPosition( attrLocationCopy );
          annotateType( srcType.getBounds().get( 0 ), Collections.singletonList( new Attribute.TypeCompound( attr.type, attr.values, posCopy ) ) );
        }
      }
    }
  }

  private TypeAnnotationPosition getTypeAnnotationPosition( List<TypeAnnotationPosition.TypePathEntry> attrLocationCopy )
  {
    TypeAnnotationPosition posCopy;
    //noinspection ConstantConditions
    if( isJava8() )
    {
      posCopy = (TypeAnnotationPosition)ReflectUtil.constructor( "com.sun.tools.javac.code.TypeAnnotationPosition" ).newInstance();
      ReflectUtil.field( posCopy, "location" ).set( attrLocationCopy );
    }
    else
    {
      posCopy = (TypeAnnotationPosition)ReflectUtil
        .method( TypeAnnotationPosition.class, "methodReceiver", List.class ).invokeStatic( attrLocationCopy );
    }
    return posCopy;
  }

  private void addAnnotation( SrcType srcType, Attribute.TypeCompound attr )
  {
    String fqn = attr.type.toString();
    if( fqn.equals( "jdk.internal.HotSpotIntrinsicCandidate" ) )
    {
      // Since java 10 we have to keep these out of stubbed java source
      return;
    }
    SrcAnnotationExpression annoExpr = new SrcAnnotationExpression( fqn );
    for( Pair<Symbol.MethodSymbol, Attribute> value: attr.values )
    {
      annoExpr.addArgument( value.fst.flatName().toString(), new SrcType( value.snd.type.toString() ), value.snd.getValue() );
    }
    srcType.addAnnotation( annoExpr );
  }

  private boolean isClassType( SrcType srcType )
  {
    return !srcType.isPrimitive() && !srcType.isArray() && !"?".equals( srcType.getName() );
  }

  private String genSuperCtorCall( IModule module, SrcClass srcClass, BasicJavacTask javacTask )
  {
    String bodyStmt;
    SrcType superClass = srcClass.getSuperClass();
    if( superClass == null )
    {
      bodyStmt = "";
    }
    else
    {
      Symbol.MethodSymbol superCtor = findConstructor( module, superClass.getFqName(), javacTask );
      if( superCtor == null )
      {
        bodyStmt = "";
      }
      else
      {
        bodyStmt = genSuperCtorCall( superCtor );
      }
    }
    return bodyStmt;
  }

  private String genSuperCtorCall( Symbol.MethodSymbol superCtor )
  {
    String bodyStmt;
    StringBuilder sb = new StringBuilder( "super(" );
    List<Symbol.VarSymbol> parameters = superCtor.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      Symbol.VarSymbol param = parameters.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( getValueForType( param.type ) );
    }
    sb.append( ");" );
    bodyStmt = sb.toString();
    return bodyStmt;
  }

  private Symbol.MethodSymbol findConstructor( IModule module, String fqn, BasicJavacTask javacTask )
  {
    manifold.util.Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( module ).getClassSymbol( javacTask, fqn );
    Symbol.ClassSymbol cs = classSymbol.getFirst();
    Symbol.MethodSymbol ctor = null;
    for( Symbol sym: cs.getEnclosedElements() )
    {
      if( sym instanceof Symbol.MethodSymbol && sym.flatName().toString().equals( "<init>" ) )
      {
        if( ctor == null )
        {
          ctor = (Symbol.MethodSymbol)sym;
        }
        else
        {
          ctor = mostAccessible( ctor, (Symbol.MethodSymbol)sym );
        }
        if( Modifier.isPublic( (int)ctor.flags() ) )
        {
          return ctor;
        }
      }
    }
    return ctor;
  }

  private Symbol.MethodSymbol mostAccessible( Symbol.MethodSymbol ctor, Symbol.MethodSymbol sym )
  {
    int ctorMods = (int)ctor.flags();
    int symMods = (int)sym.flags();
    if( Modifier.isPublic( ctorMods ) )
    {
      return ctor;
    }
    if( Modifier.isPublic( symMods ) )
    {
      return sym;
    }
    if( Modifier.isProtected( ctorMods ) )
    {
      return ctor;
    }
    if( Modifier.isProtected( symMods ) )
    {
      return sym;
    }
    if( Modifier.isPrivate( ctorMods ) )
    {
      return Modifier.isPrivate( symMods ) ? ctor : sym;
    }
    return ctor;
  }

  private void addAnnotations( SrcAnnotated<?> srcAnnotated, Symbol symbol )
  {
    for( Attribute.Compound annotationMirror: symbol.getAnnotationMirrors() )
    {
      String fqn = annotationMirror.getAnnotationType().toString();
      if( fqn.equals( "jdk.internal.HotSpotIntrinsicCandidate" ) )
      {
        // Since java 10 we have to keep these out of stubbed java source
        continue;
      }
      SrcAnnotationExpression annoExpr = new SrcAnnotationExpression( fqn );
      for( Pair<Symbol.MethodSymbol, Attribute> value: annotationMirror.values )
      {
        annoExpr.addArgument( value.fst.flatName().toString(), new SrcType( value.snd.type.toString() ), value.snd.getValue() );
      }
      srcAnnotated.addAnnotation( annoExpr );
    }
  }

  private SrcType makeTypeVarType( Symbol.TypeVariableSymbol typeVar )
  {
    StringBuilder sb = new StringBuilder( typeVar.type.toString() );
    Type lowerBound = typeVar.type.getLowerBound();
    if( lowerBound != null && !(lowerBound instanceof NullType) )
    {
      sb.append( " super " ).append( lowerBound.toString() );
    }
    else
    {
      Type upperBound = typeVar.type.getUpperBound();
      if( upperBound != null && !(upperBound instanceof NoType) && !upperBound.toString().equals( Object.class.getName() ) )
      {
        sb.append( " extends " ).append( upperBound.toString() );
      }
    }
    return new SrcType( sb.toString() );
  }

  private String getValueForType( Type type )
  {
    if( type.toString().equals( "boolean" ) )
    {
      return "false";
    }
    else if( type.isPrimitive() )
    {
      return "0";
    }
    else
    {
      return "null";
    }
  }

}
