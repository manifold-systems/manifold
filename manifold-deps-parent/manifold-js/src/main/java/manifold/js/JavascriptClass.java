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

package manifold.js;


import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.AbstractSrcMethod;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.type.SourcePosition;
import manifold.js.parser.Parser;
import manifold.js.parser.Tokenizer;
import manifold.js.parser.tree.ClassFunctionNode;
import manifold.js.parser.tree.ClassNode;
import manifold.js.parser.tree.ConstructorNode;
import manifold.js.parser.tree.Node;
import manifold.js.parser.tree.ParameterNode;
import manifold.js.parser.tree.ProgramNode;
import manifold.js.parser.tree.PropertyNode;
import manifold.api.util.ManClassUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;


import static manifold.js.JavascriptProgram.*;

public class JavascriptClass
{
  static SrcClass genClass( String fqn, ProgramNode programNode, IFile file )
  {
    ClassNode classNode = programNode.getFirstChild( ClassNode.class );

    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class )
      .imports( JavascriptClass.class )
      .imports( SourcePosition.class );

    clazz.addAnnotation(
      new SrcAnnotationExpression( SourcePosition.class )
        .addArgument( "url", String.class, programNode.getUrl().toString() )
        .addArgument( "feature", String.class, ManClassUtil.getShortClassName( fqn ) )
        .addArgument( "offset", int.class, absoluteOffset( classNode.getStart().getOffset(), file ) )
        .addArgument( "length", int.class, classNode.getEnd().getOffset() - classNode.getStart().getOffset() ) );

    String superClass = classNode.getSuperClass();
    if( superClass != null )
    {
      clazz.superClass( superClass );
    }
    clazz.addField( new SrcField( "SCOPE", ScriptableObject.class )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC | Modifier.VOLATILE )
      .initializer( "null" ) );

    clazz.addField( new SrcField( "UID", long.class )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC | Modifier.VOLATILE )
      .initializer( "0L" ) );


    clazz.addField( new SrcField( "_context", ScriptableObject.class ) );

    addConstructor( clazz, classNode, file );
    addUtilityMethods( clazz, classNode, fqn );
    addMethods( fqn, clazz, classNode, file );
    addProperties( fqn, clazz, classNode, file );

    return clazz;
  }

  private static void addConstructor( SrcClass clazz, ClassNode classNode, IFile file )
  {
    ConstructorNode constructor = classNode.getFirstChild( ConstructorNode.class );

    SrcConstructor ctor = new SrcConstructor()
      .name( classNode.getName() )
      .modifiers( Modifier.PUBLIC );

    List<SrcParameter> srcParameters;
    if( constructor != null )
    {
      srcParameters = makeSrcParameters( constructor, ctor );
      ctor.addAnnotation(
        new SrcAnnotationExpression( SourcePosition.class )
          .addArgument( "url", String.class, classNode.getProgramNode().getUrl().toString() )
          .addArgument( "feature", String.class, "constructor" )
          .addArgument( "offset", int.class, absoluteOffset( constructor.getStart().getOffset(), file ) )
          .addArgument( "length", int.class, constructor.getEnd().getOffset() - constructor.getStart().getOffset() ) );
    }
    else
    {
      srcParameters = Collections.emptyList();
    }

    //impl
    ctor.body( "_context = " + JavascriptClass.class.getSimpleName() +
               ".initInstance(getScope(), \"" + classNode.getName() + "\"" + generateArgList( srcParameters ) + ");" );

    clazz.addConstructor( ctor );
  }

  private static ThreadLocal<Long> uid = new ThreadLocal<>();

  private static void addUtilityMethods( SrcClass clazz, ClassNode classNode, String fqn )
  {
    long uid = incUid();
    SrcMethod m = new SrcMethod()
      .name( "getScope" )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC )
      .returns( ScriptableObject.class )
      .body( "if(" + uid + "L != UID) {\n" +
         "      synchronized(" + classNode.getName() + ".class) {\n" +
         "        if(" + uid + "L != UID) {\n" +
         "          UID = " + uid + "L;\n" +
         "          SCOPE = JavascriptClass.init(\"" + fqn + "\");\n" +
         "        }\n" +
         "      }\n" +
         "    }\n" +
         "    return SCOPE;" );
    clazz.addMethod( m );
  }

  private static long incUid()
  {
    Long id = uid.get();
    if( id == null )
    {
      uid.set( id = 1L );
    }
    uid.set( ++id );
    return id;
  }

  private static void addMethods( String fqn, SrcClass clazz, ClassNode classNode, IFile file )
  {
    for( ClassFunctionNode node: classNode.getChildren( ClassFunctionNode.class ) )
    {
      AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
        .name( node.getName() )
        .modifiers( Modifier.PUBLIC | (node.isStatic() ? Modifier.STATIC : 0) )
        .returns( node.getReturnType() );
      srcMethod.addAnnotation(
        new SrcAnnotationExpression( SourcePosition.class )
          .addArgument( "url", String.class, classNode.getProgramNode().getUrl().toString() )
          .addArgument( "feature", String.class, node.getName() )
          .addArgument( "offset", int.class, absoluteOffset( node.getStart().getOffset(), file ) )
          .addArgument( "length", int.class, node.getEnd().getOffset() - node.getStart().getOffset() ) );

      // params
      ParameterNode parameters = node.getFirstChild( ParameterNode.class );
      for( SrcParameter srcParameter: parameters.toParamList() )
      {
        srcMethod.addParam( srcParameter );
      }

      //impl
      if( node.isStatic() )
      {
        srcMethod.body( "return JavascriptClass.invokeStatic(getScope(), \"" +
                        ManClassUtil.getShortClassName( fqn ) + "\", \"" + node.getName() + "\"" +
                        generateArgList( parameters.toParamList() ) + ");" );
      }
      else
      {
        srcMethod.body( "return JavascriptClass.invoke(_context, \"" + node.getName() + "\"" +
                        generateArgList( parameters.toParamList() ) + ");" );
      }
      clazz.addMethod( srcMethod );
    }
  }

  private static void addProperties( String fqn, SrcClass clazz, ClassNode classNode, IFile file )
  {

    for( PropertyNode node: classNode.getChildren( PropertyNode.class ) )
    {
      final String name = node.getName();
      final String capitalizedName = name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );

      if( node.isSetter() )
      {
        AbstractSrcMethod<SrcMethod> setter = new SrcMethod()
          .name( "set" + capitalizedName )
          .modifiers( Modifier.PUBLIC | (node.isStatic() ? Modifier.STATIC : 0) )
          .addParam( "val", Object.class )
          .returns( "void" );

        setter.addAnnotation(
          new SrcAnnotationExpression( SourcePosition.class )
            .addArgument( "url", String.class, classNode.getProgramNode().getUrl().toString() )
            .addArgument( "feature", String.class, node.getName() )
            .addArgument( "offset", int.class, absoluteOffset( node.getStart().getOffset(), file ) )
            .addArgument( "length", int.class, node.getEnd().getOffset() - node.getStart().getOffset() ) );

        if( node.isStatic() )
        {
          setter.body( "JavascriptClass.setStaticProp(getScope(), \"" +
                       ManClassUtil.getShortClassName( fqn ) + "\", \"" + name + "\", val);" );
        }
        else
        {
          setter.body( "JavascriptClass.setProp(_context, \"" + name + "\", val);" );
        }
        clazz.addMethod( setter );
      }
      else
      {
        AbstractSrcMethod<SrcMethod> getter = new SrcMethod()
          .name( "get" + capitalizedName )
          .modifiers( Modifier.PUBLIC | (node.isStatic() ? Modifier.STATIC : 0) )
          .returns( node.getReturnType() );

        getter.addAnnotation(
          new SrcAnnotationExpression( SourcePosition.class )
            .addArgument( "url", String.class, classNode.getProgramNode().getUrl().toString() )
            .addArgument( "feature", String.class, node.getName() )
            .addArgument( "offset", int.class, node.getStart().getOffset() )
            .addArgument( "length", int.class, node.getEnd().getOffset() - node.getStart().getOffset() ) );

        //impl
        if( node.isStatic() )
        {
          getter.body( "return JavascriptClass.getStaticProp(getScope(), \"" +
                       ManClassUtil.getShortClassName( fqn ) + "\", \"" + name + "\");" );
        }
        else
        {
          getter.body( "return JavascriptClass.getProp(_context, \"" + name + "\");" );
        }
        clazz.addMethod( getter );
      }
    }
  }

  @SuppressWarnings("unused")
  public static <T> T invoke( ScriptableObject scope, String func, Object... args )
  {
    //noinspection unchecked
    return (T)ScriptableObject.callMethod( scope, func, args );
  }

  @SuppressWarnings("unused")
  public static <T> T invokeStatic( ScriptableObject scope, String className, String func, Object... args )
  {
    //noinspection unchecked
    return (T)ScriptableObject.callMethod( (Scriptable)scope.get( className, scope ), func, args );
  }

  @SuppressWarnings("unused")
  public static Object getProp( ScriptableObject scope, String prop )
  {
    return ScriptableObject.getProperty( scope, prop );
  }

  @SuppressWarnings("unused")
  public static Object getStaticProp( ScriptableObject scope, String className, String prop )
  {
    return ScriptableObject.getProperty( (Scriptable)scope.get( className, scope ), prop );
  }

  @SuppressWarnings("unused")
  public static void setProp( ScriptableObject scope, String prop, Object value )
  {
    ScriptableObject.putProperty( scope, prop, value );
  }

  @SuppressWarnings("unused")
  public static void setStaticProp( ScriptableObject scope, String className, String prop, Object value )
  {
    ScriptableObject.putProperty( (Scriptable)scope.get( className, scope ), prop, value );
  }

  @SuppressWarnings("unused")
  public static ScriptableObject init( String programName )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( loadSrcForName( programName, JavascriptTypeManifold.JS ) ) );
    Node programNode = parser.parse();
    ClassNode classNode = programNode.getFirstChild( ClassNode.class );
    String script = classNode.genCode();
    Context.getCurrentContext().evaluateString( scope, script, programName, 1, null );
    return scope;
  }

  @SuppressWarnings("unused")
  public static ScriptableObject initInstance( ScriptableObject scope, String name, Object... args )
  {
    return (ScriptableObject)Context.getCurrentContext().newObject( scope, name, args );
  }

  private static Object absoluteOffset( int offset, IFile file )
  {
    if( file instanceof IFileFragment )
    {
      offset += ((IFileFragment)file).getOffset();
    }
    return offset;
  }
}
