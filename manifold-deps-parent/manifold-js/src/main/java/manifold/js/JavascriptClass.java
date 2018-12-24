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
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import manifold.api.gen.AbstractSrcMethod;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
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
import manifold.util.ManClassUtil;


import static manifold.js.JavascriptProgram.*;

//## todo: replace nashorn with something else (it will be removed jdk11+ time frame)
public class JavascriptClass
{
  static SrcClass genClass( String fqn, ProgramNode programNode )
  {

    ClassNode classNode = programNode.getFirstChild( ClassNode.class );

    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class );
    clazz.addAnnotation(
      new SrcAnnotationExpression( SourcePosition.class )
        .addArgument( "url", String.class, programNode.getUrl().toString() )
        .addArgument( "feature", String.class, ManClassUtil.getShortClassName( fqn ) )
        .addArgument( "offset", int.class, classNode.getStart().getOffset() )
        .addArgument( "length", int.class, classNode.getEnd().getOffset() - classNode.getStart().getOffset() ) );

    String superClass = classNode.getSuperClass();
    if( superClass != null )
    {
      clazz.superClass( superClass );
    }
    clazz.imports( JavascriptClass.class )
      .imports( SourcePosition.class );

    clazz.addField( new SrcField( "ENGINE", ScriptEngine.class )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC | Modifier.VOLATILE )
      .initializer( new SrcRawExpression( ("null") ) ) );

    clazz.addField( new SrcField( "TIMESTAMP", long.class )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC | Modifier.VOLATILE )
      .initializer( new SrcRawExpression( ("0") ) ) );


    clazz.addField( new SrcField( "_context", ScriptObjectMirror.class ) );

    addConstructor( clazz, classNode );
    addUtilityMethods( clazz, classNode, fqn );
    addMethods( fqn, clazz, classNode );
    addProperties( fqn, clazz, classNode );

    return clazz;
  }

  private static void addConstructor( SrcClass clazz, ClassNode classNode )
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
          .addArgument( "offset", int.class, constructor.getStart().getOffset() )
          .addArgument( "length", int.class, constructor.getEnd().getOffset() - constructor.getStart().getOffset() ) );
    }
    else
    {
      srcParameters = Collections.emptyList();
    }


    //impl
    ctor.body( new SrcStatementBlock()
      .addStatement(
        new SrcRawStatement()
          .rawText( "_context = JavascriptClass.initInstance(getEngine(), \"" + classNode.getName() + "\"" + generateArgList( srcParameters ) + ");" ) ) );

    clazz.addConstructor( ctor );
  }

  private static ThreadLocal<Long> uid = new ThreadLocal<>();

  private static void addUtilityMethods( SrcClass clazz, ClassNode classNode, String fqn )
  {
    long timestamp = incUid();
    SrcMethod m = new SrcMethod()
      .name( "getEngine" )
      .modifiers( Modifier.PRIVATE | Modifier.STATIC )
      .returns( ScriptEngine.class )
      .body( new SrcStatementBlock()
        .addStatement(
          new SrcRawStatement()
            .rawText( "if( " + timestamp + "L != TIMESTAMP ) {\n" +
                      "      synchronized( " + classNode.getName() + ".class ) {\n" +
                      "        if( " + timestamp + "L != TIMESTAMP ) {\n" +
                      "          TIMESTAMP = " + timestamp + "L;\n" +
                      "          ENGINE = JavascriptClass.init(\"" + fqn + "\");\n" +
                      "        }\n" +
                      "      }\n" +
                      "    }\n" +
                      "    return ENGINE;" ) ) );
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

  private static void addMethods( String fqn, SrcClass clazz, ClassNode classNode )
  {
    for( ClassFunctionNode node : classNode.getChildren( ClassFunctionNode.class ) )
    {
      AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
        .name( node.getName() )
        .modifiers( Modifier.PUBLIC | (node.isStatic() ? Modifier.STATIC : 0) )
        .returns( node.getReturnType() );
      srcMethod.addAnnotation(
        new SrcAnnotationExpression( SourcePosition.class )
          .addArgument( "url", String.class, classNode.getProgramNode().getUrl().toString() )
          .addArgument( "feature", String.class, node.getName() )
          .addArgument( "offset", int.class, node.getStart().getOffset() )
          .addArgument( "length", int.class, node.getEnd().getOffset() - node.getStart().getOffset() ) );

      // params
      ParameterNode parameters = node.getFirstChild( ParameterNode.class );
      for( SrcParameter srcParameter : parameters.toParamList() )
      {
        srcMethod.addParam( srcParameter );
      }

      //impl
      if( node.isStatic() )
      {
        srcMethod.body( new SrcStatementBlock()
          .addStatement(
            new SrcRawStatement()
              .rawText( "return JavascriptClass.invokeStatic(getEngine(), \"" + ManClassUtil.getShortClassName( fqn ) + "\", \"" + node.getName() + "\"" + generateArgList( parameters.toParamList() ) + ");" ) ) );
      }
      else
      {
        srcMethod.body( new SrcStatementBlock()
          .addStatement(
            new SrcRawStatement()
              .rawText( "return JavascriptClass.invoke(_context, \"" + node.getName() + "\"" + generateArgList( parameters.toParamList() ) + ");" ) ) );
      }
      clazz.addMethod( srcMethod );
    }
  }

  private static void addProperties( String fqn, SrcClass clazz, ClassNode classNode )
  {

    for( PropertyNode node : classNode.getChildren( PropertyNode.class ) )
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
            .addArgument( "offset", int.class, node.getStart().getOffset() )
            .addArgument( "length", int.class, node.getEnd().getOffset() - node.getStart().getOffset() ) );

        //impl
        if( node.isStatic() )
        {
          setter.body( new SrcStatementBlock()
            .addStatement(
              new SrcRawStatement()
                .rawText( "JavascriptClass.setStaticProp(getEngine(), \"" + ManClassUtil.getShortClassName( fqn ) + "\", \"" + name + "\", val);" ) ) );
        }
        else
        {
          setter.body( new SrcStatementBlock()
            .addStatement(
              new SrcRawStatement()
                .rawText( "JavascriptClass.setProp(_context, \"" + name + "\", val);" ) ) );
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
          getter.body( new SrcStatementBlock()
            .addStatement(
              new SrcRawStatement()
                .rawText( "return JavascriptClass.getStaticProp(getEngine(), \"" + ManClassUtil.getShortClassName( fqn ) + "\", \"" + name + "\");" ) ) );
        }
        else
        {
          getter.body( new SrcStatementBlock()
            .addStatement(
              new SrcRawStatement()
                .rawText( "return JavascriptClass.getProp(_context, \"" + name + "\");" ) ) );
        }
        clazz.addMethod( getter );
      }
    }
  }

  /* implementation */
  public static <T> T invoke( ScriptObjectMirror context, String func, Object... args )
  {
    return (T)context.callMember( func, args );
  }

  public static <T> T invokeStatic( ScriptEngine engine, String className, String func, Object... args )
  {
    ScriptObjectMirror classObject = getClassObject( engine, className );
    return (T)classObject.callMember( func, args );
  }

  public static Object getProp( ScriptObjectMirror context, String prop )
  {
    return context.get( prop );
  }

  public static Object getStaticProp( ScriptEngine engine, String className, String property )
  {
    ScriptObjectMirror classObject = getClassObject( engine, className );
    return classObject.get( property );
  }

  public static void setProp( ScriptObjectMirror context, String prop, Object value )
  {
    context.setMember( prop, value );
  }

  public static void setStaticProp( ScriptEngine engine, String className, String property, Object value )
  {
    ScriptObjectMirror classObject = getClassObject( engine, className );
    classObject.put( "_" + property, value ); //TODO why the underscore?
  }

  public static ScriptEngine init( String programName )
  {
    ScriptEngine nashorn = new ScriptEngineManager().getEngineByName( "nashorn" );
    nashorn.setBindings( new ThreadSafeBindings(), ScriptContext.ENGINE_SCOPE );
    Parser parser = new Parser( new Tokenizer( loadSrcForName( programName, JavascriptTypeManifold.JS ) ) );
    Node programNode = parser.parse();
    ClassNode classNode = programNode.getFirstChild( ClassNode.class );
    String script = classNode.genCode();
    try
    {
      nashorn.eval( script );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
    return nashorn;
  }

  public static ScriptObjectMirror initInstance( ScriptEngine engine, String name, Object... args )
  {
    JSObject classObject = getClassObject( engine, name );
    return (ScriptObjectMirror)classObject.newObject( args );
  }

  private static ScriptObjectMirror getClassObject( ScriptEngine engine, String name )
  {
    return (ScriptObjectMirror)((ScriptObjectMirror)engine.get( "nashorn.global" )).get( name );
  }

}
