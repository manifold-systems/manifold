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
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.Array;
import com.sun.tools.javac.code.Attribute.Class;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Attribute.Enum;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import manifold.api.gen.*;
import manifold.api.gen.AbstractSrcClass.Kind;
import manifold.api.host.IModule;
import manifold.api.util.JavacUtil;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;


import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static com.sun.tools.javac.code.Flags.DEFAULT;
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

  public SrcClass makeStub( String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, IModule module, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return makeStub( fqn, classSymbol, compilationUnit, javacTask, module, location, errorHandler, true );
  }

  public SrcClass makeStub( String fqn, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, IModule module, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler,
                            boolean withMembers )
  {
    return makeSrcClass( fqn, null, classSymbol, compilationUnit, javacTask, module, location, errorHandler, withMembers );
  }

  private SrcClass makeSrcClass( String fqn, SrcClass enclosing, Symbol.ClassSymbol classSymbol, CompilationUnitTree compilationUnit, BasicJavacTask javacTask, IModule module, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler, boolean withMembers )
  {
    SrcClass srcClass;
    if( enclosing == null )
    {
      srcClass = new SrcClass( fqn, getKindFrom( classSymbol ), location, module, errorHandler )
        .modifiers( getClassSymbolModifiers( classSymbol ) );
    }
    else
    {
      srcClass = new SrcClass( fqn, enclosing, getKindFrom( classSymbol ) )
        .modifiers( getClassSymbolModifiers( classSymbol ) );
    }
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
      if( iface.tsym.getQualifiedName().toString().startsWith( "java.lang.constant." ) )
      {
        // android: for some reason android does not resolve this package, i don't like you android
        continue;
      }
      srcClass.addInterface( makeNestedType( iface ) );
    }
    if( withMembers )
    {
      java.util.List<Symbol> members = classSymbol.getEnclosedElements();
      for( Symbol sym: members )
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
          addField( srcClass, (Symbol.VarSymbol)sym );
        }
        else if( sym instanceof Symbol.MethodSymbol )
        {
          if( !isEnumMethod( sym ) && !isExpired( sym ) )
          {
            addMethod( module, srcClass, (Symbol.MethodSymbol)sym, javacTask );
          }
        }
      }

      addDefaultCtorForEnum( classSymbol, srcClass, members );
    }
    return srcClass;
  }

  private static Set<javax.lang.model.element.Modifier> getClassSymbolModifiers( Symbol.ClassSymbol classSymbol )
  {
    long modifiers = classSymbol.flags() & ~DEFAULT;
    if( classSymbol.isEnum() )
    {
      // enums can be weirdly internally abstract where it can have abstract methods that are implemented anonymously.
      // However, since an enum's instances are limited to its values, it does not declare itself as abstract even though
      // internally it is recorded as such.
      // public enum MyEnum {
      //    A() { String foo() {return "hi";} };
      //    abstract String foo();
      // }
      modifiers = modifiers & ~ABSTRACT;
    }
    return Flags.asModifierSet( modifiers );
  }

  /*
   * Note, we have to check for the java.lang.Record super class because we get the ClassSymbol using Java 8,
   * which does not have records
   */
  private AbstractSrcClass.Kind getKindFrom( Symbol.ClassSymbol classSymbol )
  {
    if( !classSymbol.isInterface() && JreUtil.isJava16orLater() )
    {
      Type superclass = classSymbol.getSuperclass();
      if( superclass != null && superclass.toString().equals( "java.lang.Record" ) )
      {
        return AbstractSrcClass.Kind.Record;
      }
    }
    return SrcClass.Kind.from( classSymbol.getKind() );
  }

  private void addDefaultCtorForEnum( Symbol.ClassSymbol classSymbol, SrcClass srcClass, java.util.List<Symbol> members )
  {
    if( !classSymbol.isEnum() )
    {
      return;
    }

    if( members.stream().noneMatch( e ->
      e.isConstructor() && e instanceof Symbol.MethodSymbol && ((Symbol.MethodSymbol)e).getParameters().isEmpty() ) )
    {
      // Add default no-arg ctor because enum constant stubs do not call a ctor explicitly
      SrcMethod srcMethod = new SrcMethod( srcClass, true );
      srcMethod.body( new SrcStatementBlock()
        .addStatement(
          new SrcRawStatement()
            .rawText( "throw new RuntimeException();" ) ) );
      srcClass.addMethod( srcMethod );
    }
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
    try
    {
      SrcClass innerClass = makeSrcClass( sym.getQualifiedName().toString(), srcClass, (Symbol.ClassSymbol)sym, null, javacTask, module, null, null, true );
      srcClass.addInnerClass( innerClass );
    }
    catch( NullPointerException npe )
    {
      //todo:
      // This happens when an inner class extends a class that is not accessible from Manifold for unknown reasons. An
      // extension class on java.awt.Component in jdk 21 demonstrates this. Most of the time the inner class can be
      // ignored bc it is usually private and not referenced in declarations. Anyhow, the inaccessible class should be
      // made accessible via reflection, one way or another. Make this work.
      System.err.println( "Warning: Failed to generate inner class: " + sym.getQualifiedName() );
    }
  }

  private void addField( SrcClass srcClass, Symbol.VarSymbol sym )
  {
    SrcField srcField = new SrcField( sym.name.toString(), makeSrcType( sym.type, sym, TargetType.FIELD, -1 ) );
    if( sym.isEnum() )
    {
      srcField.enumConst();
      srcClass.addEnumConst( srcField );
    }
    else
    {
      srcField.modifiers( sym.getModifiers() );
      if( Modifier.isFinal( (int)srcField.getModifiers() ) )
      {
        Object constValue = sym.getConstantValue();
        if( constValue == null )
        {
          constValue = getValueForType( sym.type );
        }
        else
        {
          constValue = "(" + sym.type + ")" + qualifyConstantValue( constValue );
        }
        srcField.initializer( (String)constValue );
      }
      srcClass.addField( srcField );
    }
  }

  private String qualifyConstantValue( Object constValue )
  {
    String value = String.valueOf( constValue );
    if( constValue instanceof Long )
    {
      value += "L";
    }
    else if( constValue instanceof Float )
    {
      if( Float.isInfinite( (Float)constValue ) )
      {
        String infinite = "1.0f / 0.0f";
        if( value.charAt( 0 ) == '-' )
        {
          infinite = '-' + infinite;
        }
        value = infinite;
      }
      else if( Float.isNaN( (Float)constValue ) )
      {
        value = "0.0f / 0.0f";
      }
      else
      {
        value += "f";
      }
    }
    else if( constValue instanceof Double )
    {
      if( Double.isInfinite( (Double)constValue ) )
      {
        String infinite = "1.0 / 0.0";
        if( value.charAt( 0 ) == '-' )
        {
          infinite = '-' + infinite;
        }
        value = infinite;
      }
      else if( Double.isNaN( (Double)constValue ) )
      {
        value = "0.0d / 0.0";
      }
      else
      {
        value += "d";
      }
    }
    else if( constValue instanceof String )
    {
      value = '"' + ManEscapeUtil.escapeForJavaStringLiteral( value ) + '"';
    }
    else if( constValue instanceof Character )
    {
      value = "'" + ManEscapeUtil.escapeForJava( (char)constValue ) + "'";
    }
    return value;
  }

  private void addMethod( IModule module, SrcClass srcClass, Symbol.MethodSymbol method, BasicJavacTask javacTask )
  {
    String name = method.flatName().toString();
    SrcMethod srcMethod = new SrcMethod( srcClass, name.equals( "<init>" ) );
    addAnnotations( srcMethod, method );
    srcMethod.modifiers( getModifiers( method ) );
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
      removeMethodAnnotationsIntendedForReturnType( srcMethod );
    }
    for( Symbol.TypeVariableSymbol typeVar: method.getTypeParameters() )
    {
      srcMethod.addTypeVar( makeTypeVarType( typeVar ) );
    }
    java.util.List<Symbol.VarSymbol> recordFields = null;
    Symbol.ClassSymbol owner = (Symbol.ClassSymbol)method.owner;
    if( method.isConstructor() && getKindFrom( owner ) == AbstractSrcClass.Kind.Record )
    {
      Symbol.MethodSymbol primaryRecordCtor = findPrimaryRecordCtor( owner, javacTask );
      if( primaryRecordCtor == method )
      {
        recordFields = getRecordComponents( owner );
      }
    }
    List<Symbol.VarSymbol> parameters = method.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      Symbol.VarSymbol param = parameters.get( i );
      if( param.type.toString().equals( "java.lang.AbstractStringBuilder" ) )
      {
        // android: doesn't resolve AbstractStringBuilder with extended classes while compiling their source, such as when String is extended locally
        return;
      }
      String paramName = recordFields == null ? param.flatName().toString() : recordFields.get( i ).flatName().toString();
      SrcParameter srcParam = new SrcParameter( paramName, makeSrcType( param.type, method, TargetType.METHOD_FORMAL_PARAMETER, i ) );
      srcMethod.addParam( srcParam );
      addAnnotations( srcParam, param );
    }
    removeParamAnnotationsIntendedForParamType( srcMethod );
    List<Type> thrownTypes = method.getThrownTypes();
    for( int i = 0; i < thrownTypes.size(); i++ )
    {
      Type throwType = thrownTypes.get( i );
      srcMethod.addThrowType( makeSrcType( throwType, method, TargetType.THROWS, i ) );
    }

    setAnnotationDefaultValue( method, srcMethod );

    String bodyStmt;
    if( srcMethod.isConstructor() && !srcClass.isEnum() )
    {
      // Note we can't just throw an exception for the ctor body, the compiler will
      // still complain about the missing super() call if the super class does not have
      // an accessible default ctor. To appease the compiler we generate a super(...)
      // call to the first accessible constructor we can find in the super class.
      if( srcClass.isRecord() )
      {
        bodyStmt = genRecordCtorBody( srcMethod, method, javacTask );
      }
      else
      {
        bodyStmt = genSuperCtorCall( module, srcClass, javacTask );
      }
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

  private void setAnnotationDefaultValue( Symbol.MethodSymbol method, SrcMethod srcMethod )
  {
    if( (method.owner.flags_field & Flags.ANNOTATION) == 0 )
    {
      // declaring class is not an annotation
      return;
    }

    Attribute defaultValue = method.getDefaultValue();
    if( defaultValue == null )
    {
      // annotation method does not have a default value
      return;
    }

    String qualifiedValue = defaultValue.toString();
    if( defaultValue instanceof Attribute.Enum )
    {
      // enum constants must be qualified

      Symbol.VarSymbol value = ((Attribute.Enum)defaultValue).getValue();
      qualifiedValue = value.enclClass() + "." + value;
    }
    srcMethod.setDefaultValue( qualifiedValue );
  }

  private static Set<javax.lang.model.element.Modifier> getModifiers( Symbol.MethodSymbol method )
  {
    long flags = method.flags();
    if( method.owner.getKind() == ElementKind.ENUM )
    {
      // abstract enum methods can't really be abstract
      flags = flags & ~ABSTRACT;
    }
    return Flags.asModifierSet( (flags & DEFAULT) != 0 ? flags & ~ABSTRACT : flags );
  }

  /**
   * Prevent "error: Xxx is not a repeatable annotation type" when eg., jetbrtains Nullable is used in classes
   * that have extensions -- the MethodSymbol erroneously claims the annotation is on both the method and the ret type,
   * maybe to be backward compatible with older processors that don't handle annotated types (?)  Anyhow, we remove the
   * duplicate from the method, since the return type is more specific and since the compiler is just gonna put it back
   * on the method when it compiles the augmented class to produce the ClassSymbol with extensions.
   */
  private void removeMethodAnnotationsIntendedForReturnType( SrcMethod srcMethod )
  {
    SrcType returnType = srcMethod.getReturnType();
    java.util.List<SrcAnnotationExpression> retAnnos = returnType.getAnnotations();
    java.util.List<SrcAnnotationExpression> methAnnos = srcMethod.getAnnotations();
    for( SrcAnnotationExpression anno: retAnnos )
    {
      for( int i = 0; i < methAnnos.size(); i++ )
      {
        SrcAnnotationExpression methAnno = methAnnos.get( i );
        if( methAnno.toString().equals( anno.toString() ) )
        {
          methAnnos.remove( i );
          break;
        }
      }
    }
  }

  /**
   * Similar to {@link #removeMethodAnnotationsIntendedForReturnType} but applies to parameters and parameter types.
   */
  private void removeParamAnnotationsIntendedForParamType( SrcMethod srcMethod )
  {
    for( SrcParameter param: srcMethod.getParameters() )
    {
      SrcType paramType = param.getType();
      java.util.List<SrcAnnotationExpression> paramAnnos = param.getAnnotations();
      java.util.List<SrcAnnotationExpression> typeAnnos = paramType.getAnnotations();
      for( SrcAnnotationExpression typeAnno: typeAnnos )
      {
        for( int i = 0; i < paramAnnos.size(); i++ )
        {
          SrcAnnotationExpression paramAnno = paramAnnos.get( i );
          if( paramAnno.toString().equals( typeAnno.toString() ) )
          {
            paramAnnos.remove( i );
            break;
          }
        }
      }
    }
  }

  private SrcType makeSrcType( Type type, Symbol symbol, TargetType targetType, int index )
  {
    SrcType srcType;
    List<Attribute.TypeCompound> annotationMirrors = type.getAnnotationMirrors();
    if( annotationMirrors != null && !annotationMirrors.isEmpty() )
    {
      String unannotatedType = unannotatedType( type ).toString();
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
      .filter( attr -> attr.position.type == targetType && isTargetIndex( targetType, attr, index ) )
      .collect( Collectors.toList() );

    annotateType( srcType, targetedTypeAttrs );
    return srcType;
  }

  private Type unannotatedType( Type type )
  {
    return isJava8()
           ? (Type)ReflectUtil.method( type, "unannotatedType" ).invoke()
           : (Type)ReflectUtil.method( type, "stripMetadata" ).invoke();
  }

  private String typeNoAnnotations( Type type )
  {
    if( isJava8() )
    {
      if( type instanceof Type.ArrayType )
      {
        return typeNoAnnotations( unannotatedType( ((Type.ArrayType)type).getComponentType() ) ) + "[]";
      }
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
        return attr.position.parameter_index == index;

      case THROWS:
        return attr.position.type_index == index;

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
        TypeAnnotationPosition attrPos = attr.position;
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
        TypeAnnotationPosition attrPos = attr.position;
        if( attrPos == null )
        {
          return;
        }
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
          TypeAnnotationPosition posCopy = getTypeAnnotationPosition( attrLocationCopy );
          annotateType( srcType.getBounds().get( 0 ), Collections.singletonList( new Attribute.TypeCompound( attr.type, attr.values, posCopy ) ) );
        }
      }
    }
  }

  public static TypeAnnotationPosition getTypeAnnotationPosition( List<TypeAnnotationPosition.TypePathEntry> attrLocationCopy )
  {
    TypeAnnotationPosition posCopy;
    if( isJava8() )
    {
      posCopy = (TypeAnnotationPosition)ReflectUtil.constructor( "com.sun.tools.javac.code.TypeAnnotationPosition" ).newInstance();
      ReflectUtil.field( posCopy, "location" ).set( attrLocationCopy );
    }
    else
    {
      posCopy = (TypeAnnotationPosition)ReflectUtil
        .method( TypeAnnotationPosition.class, "methodReceiver", List.class )
        .invokeStatic( attrLocationCopy );
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
    if( fqn.startsWith( "jdk.internal.vm.annotation." ) )
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

  private String genRecordCtorBody( SrcMethod srcMethod, Symbol.MethodSymbol method, BasicJavacTask javacTask )
  {
    Symbol.ClassSymbol owner = (Symbol.ClassSymbol)method.owner;
    Symbol.MethodSymbol primaryRecordCtor = findPrimaryRecordCtor( (Symbol.ClassSymbol)method.owner, javacTask );
    if( primaryRecordCtor == method )
    {
      srcMethod.setPrimaryConstructor( true );
      return initializeRecordFields( owner );
    }
    return callPrimaryRecordCtor( owner, primaryRecordCtor );
  }

  private String callPrimaryRecordCtor( Symbol.ClassSymbol owner, Symbol.MethodSymbol primaryRecordCtor )
  {
    StringBuilder sb = new StringBuilder( "this(" );
    List<Symbol.VarSymbol> parameters = primaryRecordCtor.getParameters();
    for( int i = 0, parametersSize = parameters.size(); i < parametersSize; i++ )
    {
      Symbol.VarSymbol param = parameters.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( getValueForType( param.type ) );
    }
    sb.append( ");\n" );
    return sb.toString();
  }

  private Symbol.MethodSymbol findPrimaryRecordCtor( Symbol.ClassSymbol owner, BasicJavacTask javacTask )
  {
    java.util.List<Symbol.VarSymbol> fields = getRecordComponents( owner );
    for( Symbol meth : IDynamicJdk.instance().getMembers( owner, m -> m instanceof Symbol.MethodSymbol && m.isConstructor() ) )
    {
      Symbol.MethodSymbol method = (Symbol.MethodSymbol)meth;
      List<Symbol.VarSymbol> params = method.getParameters();
      if( params.size() == fields.size() )
      {
        boolean primaryCtor = true;
        Types types = Types.instance( javacTask.getContext() );
        for( int i = 0, paramsSize = params.size(); i < paramsSize; i++ )
        {
          Symbol.VarSymbol param = params.get( i );
          if( !types.isSameType( param.type, fields.get( i ).type ) )
          {
            primaryCtor = false;
            break;
          }
        }
        if( primaryCtor )
        {
          return method;
        }
      }
    }
    return null;
  }

  private String initializeRecordFields( Symbol.ClassSymbol owner )
  {
    StringBuilder sb = new StringBuilder();
    java.util.List<Symbol.VarSymbol> fields = getRecordComponents( owner );
    for( Symbol f: fields )
    {
      sb.append( "this." ).append( f.flatName() ).append( " = " ).append( getValueForType( f.type ) ).append( "; " );
    }
    sb.append( "\n" );
    return sb.toString();
  }

  private java.util.List<Symbol.VarSymbol> getRecordComponents( Symbol.ClassSymbol owner )
  {
    // in declared order
    return new ArrayList( (List<?>)ReflectUtil.method( owner, "getRecordComponents" ).invoke() );
  }

  private Symbol.MethodSymbol findConstructor( IModule module, String fqn, BasicJavacTask javacTask )
  {
    manifold.rt.api.util.Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( module ).getClassSymbol( javacTask, fqn );
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
      if( fqn.startsWith( "jdk.internal.vm.annotation." ) )
      {
        // Since java 12 we have to keep these out of stubbed java source
        continue;
      }
      if( fqn.startsWith( "<" ) )
      {
        // android: avoid errant types such as "<any>"
        continue;
      }
      if( fqn.equals( "jdk.internal.ValueBased" ) )
      {
        // Since java 16 we have to keep these out of stubbed java source
        continue;
      }
      //noinspection IfCanBeSwitch
      if( fqn.equals( "android.annotation.Nullable" ) )
      {
        // retarded android bullshit
//        fqn = "androidx.annotation.RecentlyNullable";
        continue;
      }
      else if( fqn.equals( "android.annotation.NonNull" ) )
      {
        // retarded android bullshit
//        fqn = "androidx.annotation.RecentlyNonNull";
        continue;
      }
      else if( fqn.equals( "androidx.annotation.RecentlyNullable" ) )
      {
        // retarded android bullshit
//        fqn = "androidx.annotation.RecentlyNullable";
        continue;
      }
      else if( fqn.equals( "androidx.annotation.RecentlyNonNull" ) )
      {
        // retarded android bullshit
//        fqn = "androidx.annotation.RecentlyNonNull";
        continue;
      }
      SrcAnnotationExpression annoExpr = new SrcAnnotationExpression( fqn );
      boolean multipleAnnotations = false;
      for( Pair<Symbol.MethodSymbol, Attribute> value : annotationMirror.values )
      {
        addArguments( annoExpr, value.fst, value.snd );
        if( value.snd instanceof Attribute.Array )
        {
          multipleAnnotations = true;
          ( (SrcAnnotationArrayExpression) annoExpr.getArguments().get( 0 ).getValue() ).getArguments()
            .forEach( arg -> srcAnnotated.addAnnotation( (SrcAnnotationExpression) arg.getValue() ) );
        }
      }
      if( !multipleAnnotations )
      {
        srcAnnotated.addAnnotation( annoExpr );
      }
    }
  }

  /**
   * Adds the arguments from the provided  symbol and attribute to the given annotation expression.
   * This method handles various types of attributes, including arrays, classes, enums, and constants, and correctly
   * formats them for use in the annotation expression.
   * <p>
   * The method recursively processes compound attributes and constructs the appropriate annotation
   * arguments, including handling nested structures for array and compound attribute types.
   *
   * @param annoExpr the annotation expression to which the arguments will be added.
   * @param symbol the symbol representing part of the original annotation being processed.
   * @param attribute the attribute whose value(s) will be added as arguments to the annotation.
   */
  private void addArguments( SrcAnnotationExpression annoExpr, Symbol symbol, Attribute attribute )
  {
    SrcType srcType = makeSrcType( attribute.type );
    String name = symbol.name.toString();
    if( attribute instanceof Attribute.Array )
    {
      Attribute[] values = ( (Array) attribute ).values;
      SrcAnnotationArrayExpression annoArrayExpr = new SrcAnnotationArrayExpression( srcType.toString() );
      for( Attribute value : values )
      {
        if( value instanceof Attribute.Compound )
        {
          SrcAnnotationExpression annoExprInner = new SrcAnnotationExpression( srcType.getFqName() );
          for( Pair<Symbol.MethodSymbol, Attribute> val : ( (Attribute.Compound) value ).values )
          {
            addArguments( annoExprInner, val.fst, val.snd );
          }
          SrcArgument srcArgument = new SrcArgument( annoExprInner );
          annoArrayExpr.addArgument( srcArgument );
        } else if( value instanceof Attribute.Class )
        {
          SrcArgument srcParameter = new SrcArgument( makeSrcType( value.type ), ( (Class) value ).classType.toString() + ".class" );
          annoArrayExpr.addArgument( srcParameter );
        } else if( value instanceof Attribute.Enum )
        {
          SrcArgument srcParameter = new SrcArgument( makeSrcType( value.type ), ( (Enum) value ).value.toString() );
          annoArrayExpr.addArgument( srcParameter );
        } else if( value instanceof Attribute.Constant )
        {
          SrcArgument srcParameter = new SrcArgument( makeSrcType( value.type ), ( (Attribute.Constant) value ).value.toString() );
          annoArrayExpr.addArgument( srcParameter );
        }
      }
      SrcArgument srcArgument = new SrcArgument( annoArrayExpr ).name( name );
      annoExpr.addArgument( srcArgument );
    } else if( attribute instanceof Attribute.Class )
    {
      SrcIdentifier srcIdentifier = new SrcIdentifier( ( (Class) attribute ).classType.toString() + ".class" );
      annoExpr.addArgument( name, srcType, srcIdentifier );
    } else if( attribute instanceof Attribute.Enum )
    {
      SrcIdentifier srcIdentifier = new SrcIdentifier( ( (Enum) attribute ).value.toString() );
      annoExpr.addArgument( name, srcType, srcIdentifier );
    } else if( attribute instanceof Attribute.Constant )
    {
      annoExpr.addArgument( name, srcType, attribute.getValue().toString() );
    }
  }

  private static SrcType makeSrcType( Type t )
  {
    SrcType type = new SrcType( t.toString() );
    if( t.tsym != null )
    {
      // If component type is an Enum, ensure it is set as such.
      // This indicates that unqualified enum values sb qualified with the component type name when rendered.

      Symbol.TypeSymbol tsym = t.tsym;
      SrcType compType = type;
      while( t instanceof Type.ArrayType )
      {
        t = ((Type.ArrayType)t).getComponentType();
        tsym = t.tsym;
        compType = compType.getComponentType();
      }
      if( tsym != null && tsym.isEnum() )
      {
        compType.setEnum( true );
      }
    }
    return type;
  }

  private boolean isExpired( Symbol symbol )
  {
    for( Attribute.Compound annotationMirror: symbol.getAnnotationMirrors() )
    {
      String fqn = annotationMirror.getAnnotationType().toString();
      if( fqn.equals( "manifold.ext.rt.api.Expires" ) )
      {
        for( Pair<Symbol.MethodSymbol, Attribute> value: annotationMirror.values )
        {
          return JavacUtil.getReleaseNumber() >= (int)value.snd.getValue();
        }
      }
    }
    return false;
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

  // These values substitute NON-compile-time constant initializers, thus they are never used at runtime (because
  // extension stub classes are not used at runtime).
  private String getValueForType( Type type )
  {
    String value;
    if( type.isPrimitive() )
    {
      switch( type.getKind() )
      {
        case BOOLEAN:
          value = "(boolean)Boolean.valueOf(true)";
          break;
        case BYTE:
          value = "(byte)Byte.valueOf((byte)0)";
          break;
        case SHORT:
          value = "(short)Short.valueOf((short)0)";
          break;
        case INT:
          value = "(int)Integer.valueOf(0)";
          break;
        case LONG:
          value = "(long)Long.valueOf(0)";
          break;
        case CHAR:
          value = "(char)Character.valueOf((char)0)";
          break;
        case FLOAT:
          value = "(float)Float.valueOf(0f)";
          break;
        case DOUBLE:
          value = "(double)Double.valueOf(0d)";
          break;
        default:
          throw new IllegalStateException();
      }
    }
    else
    {
      String fqn = type.toString();
      if( type instanceof Type.TypeVar )
      {
        value = "null";
      }
      else
      {
        fqn = removeGenerics( fqn );
        value = "(" + fqn + ") null"; // cast to disambiguate when used as an argument
      }
    }
    return value;
  }

  public String removeGenerics( String type )
  {
    String rawType = type;
    int iAngle = type.indexOf( "<" );
    if( iAngle > 0 )
    {
      rawType = rawType.substring( 0, iAngle );
      int iLastAngle = type.lastIndexOf( '>' );
      if( type.length()-1 > iLastAngle )
      {
        // array brackets
        rawType += type.substring( iLastAngle + 1 );
      }
    }
    return rawType;
  }
}
