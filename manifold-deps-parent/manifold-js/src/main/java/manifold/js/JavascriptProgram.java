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
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import manifold.api.fs.IFile;
import manifold.api.gen.AbstractSrcMethod;
import manifold.api.gen.SrcAnnotated;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.type.ITypeManifold;
import manifold.api.type.SourcePosition;
import manifold.internal.host.RuntimeManifoldHost;
import manifold.js.parser.Parser;
import manifold.js.parser.Tokenizer;
import manifold.js.parser.tree.FunctionNode;
import manifold.js.parser.tree.Node;
import manifold.js.parser.tree.ParameterNode;
import manifold.js.parser.tree.ProgramNode;


import static manifold.js.Util.safe;

public class JavascriptProgram
{

  /* codegen */
  static SrcClass genProgram( String fqn, ProgramNode programNode )
  {
    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class ).superClass( JavascriptProgram.class )
      .imports( SourcePosition.class );

    clazz.addField( new SrcField( "ENGINE", ScriptEngine.class )
      .modifiers( Modifier.STATIC )
      .initializer( new SrcRawExpression( ("init(\"" + fqn + "\")") ) ) );

    clazz.addConstructor( new SrcConstructor().modifiers( Modifier.PRIVATE ).body( new SrcStatementBlock() ) );

    for( FunctionNode node : programNode.getChildren( FunctionNode.class ) )
    {
      AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
        .name( node.getName() )
        .modifiers( Modifier.STATIC | Modifier.PUBLIC )
        .returns( node.getReturnType() );

      List<SrcParameter> srcParameters = makeSrcParameters( node, srcMethod );
      srcMethod.body( new SrcStatementBlock()
        .addStatement(
          new SrcRawStatement()
            .rawText( "return invoke(ENGINE, \"" + node.getName() + "\"" + generateArgList( srcParameters ) + ");" ) ) );
      clazz.addMethod( srcMethod );

    }
    return clazz;
  }

  static List<SrcParameter> makeSrcParameters( Node node, SrcAnnotated srcMethod )
  {
    ParameterNode paramNode = node.getFirstChild( ParameterNode.class );
    List<SrcParameter> srcParameters = paramNode != null ? paramNode.toParamList() : Collections.emptyList();
    for( SrcParameter srcParameter : srcParameters )
    {
      srcMethod.addParam( srcParameter );
    }
    return srcParameters;
  }

  static String generateArgList( List<SrcParameter> srcParameters )
  {
    StringBuilder sb = new StringBuilder();
    for( SrcParameter srcParameter : srcParameters )
    {
      sb.append( "," );
      sb.append( srcParameter.getSimpleName() );
    }
    return sb.toString();
  }

  /* implementation */
  public static <T> T invoke( ScriptEngine engine, String func, Object... args )
  {
    try
    {
      return (T)((Invocable)engine).invokeFunction( func, args );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public static ScriptEngine init( String programName )
  {
    ScriptEngine nashorn = new ScriptEngineManager().getEngineByName( "nashorn" );
    nashorn.setBindings( new ThreadSafeBindings(), ScriptContext.ENGINE_SCOPE );
    Parser parser = new Parser( new Tokenizer( loadSrcForName( programName, JavascriptTypeManifold.JS ) ) );
    Node programNode = parser.parse();
    safe( () -> nashorn.eval( programNode.genCode() ) );
    return nashorn;
  }

  static IFile loadSrcForName( String fqn, String fileExt )
  {
    List<IFile> filesForType = findJavascriptManifold( fileExt ).findFilesForType( fqn );
    if( filesForType.isEmpty() )
    {
      throw new IllegalStateException( "Could not find a ." + fileExt + " file for type: " + fqn );
    }
    if( filesForType.size() > 1 )
    {
      System.err.println( "===\nWARNING: more than one ." + fileExt + " file corresponds with type: '" + fqn + "':\n");
      filesForType.forEach( file -> System.err.println( file.toString() ) );
      System.err.println( "using the first one: " + filesForType.get( 0 ) + "\n===" );
    }
    return filesForType.get( 0 );
  }

  private static ITypeManifold findJavascriptManifold( String fileExt )
  {
    ITypeManifold tm = RuntimeManifoldHost.get().getSingleModule().getTypeManifolds().stream()
      .filter( e -> e.handlesFileExtension( fileExt ) )
      .findFirst().orElse( null );
    if( tm == null )
    {
      throw new IllegalStateException( "Could not find type manifold for extension: " + fileExt );
    }
    return tm;
  }
}
