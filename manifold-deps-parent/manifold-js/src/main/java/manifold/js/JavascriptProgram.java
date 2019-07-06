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
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.fs.def.FileFragmentImpl;
import manifold.api.gen.AbstractSrcMethod;
import manifold.api.gen.SrcAnnotated;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.type.FragmentValue;
import manifold.api.type.ITypeManifold;
import manifold.api.type.SourcePosition;
import manifold.internal.host.RuntimeManifoldHost;
import manifold.js.parser.Parser;
import manifold.js.parser.Tokenizer;
import manifold.js.parser.tree.FunctionNode;
import manifold.js.parser.tree.Node;
import manifold.js.parser.tree.ParameterNode;
import manifold.js.parser.tree.ProgramNode;
import manifold.util.ManEscapeUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;


public class JavascriptProgram
{
  static SrcClass genProgram( String fqn, ProgramNode programNode, IFile file )
  {
    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class ).superClass( JavascriptProgram.class )
      .imports( SourcePosition.class )
      .imports( FragmentValue.class );

    if( file instanceof IFileFragment )
    {
      clazz.addAnnotation( new SrcAnnotationExpression( FragmentValue.class.getSimpleName() )
        .addArgument( "methodName", String.class, "fragmentValue" )
        .addArgument( "type", String.class, Object.class.getTypeName() ) );

      String url;
      try
      {
        url = ((IFileFragment)file).getEnclosingFile().toURI().toURL().toString();
      }
      catch( MalformedURLException e )
      {
        throw new RuntimeException( e );
      }
      clazz.addField( new SrcField( "SCOPE", ScriptableObject.class )
        .modifiers( Modifier.STATIC )
        .initializer( "initDirect(\"" + ManEscapeUtil.escapeForJava(
          ((FileFragmentImpl)file).getContent() ) + "\", \"" + url + "\")" ) );

      AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
        .name( "fragmentValue" )
        .modifiers( Modifier.STATIC | Modifier.PUBLIC )
        .returns( Object.class.getSimpleName() );
      srcMethod.body( "return evaluate(\"" + ManEscapeUtil.escapeForJava(
        ((FileFragmentImpl)file).getContent() ) + "\", \"" + url + "\");" );
      clazz.addMethod( srcMethod );

    }
    else
    {
      clazz.addField( new SrcField( "SCOPE", ScriptableObject.class )
        .modifiers( Modifier.STATIC )
        .initializer( "init(\"" + fqn + "\")" ) );
    }

    clazz.addConstructor( new SrcConstructor().modifiers( Modifier.PRIVATE ).body( new SrcStatementBlock() ) );

    for( FunctionNode node: programNode.getChildren( FunctionNode.class ) )
    {
      AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
        .name( node.getName() )
        .modifiers( Modifier.STATIC | Modifier.PUBLIC )
        .returns( node.getReturnType() );
      srcMethod.addAnnotation(
        new SrcAnnotationExpression( SourcePosition.class )
          .addArgument( "url", String.class, programNode.getUrl().toString() )
          .addArgument( "feature", String.class, node.getName() )
          .addArgument( "offset", int.class, absoluteOffset( node.getStart().getOffset(), file ) )
          .addArgument( "length", int.class, node.getEnd().getOffset() - node.getStart().getOffset() ) )
        .body( "return invoke(SCOPE, \"" + node.getName() + "\"" +
               generateArgList( makeSrcParameters( node, srcMethod ) ) + ");" );
      clazz.addMethod( srcMethod );
    }
    return clazz;
  }

  static List<SrcParameter> makeSrcParameters( Node node, SrcAnnotated srcMethod )
  {
    ParameterNode paramNode = node.getFirstChild( ParameterNode.class );
    List<SrcParameter> srcParameters = paramNode != null ? paramNode.toParamList() : Collections.emptyList();
    for( SrcParameter srcParameter: srcParameters )
    {
      srcMethod.addParam( srcParameter );
    }
    return srcParameters;
  }

  static String generateArgList( List<SrcParameter> srcParameters )
  {
    StringBuilder sb = new StringBuilder();
    for( SrcParameter srcParameter: srcParameters )
    {
      sb.append( "," );
      sb.append( srcParameter.getSimpleName() );
    }
    return sb.toString();
  }

  @SuppressWarnings("unused")
  public static <T> T invoke( ScriptableObject scope, String func, Object... args )
  {
    try
    {
      Function renderToString = (Function)scope.get( func, scope );
      //noinspection unchecked
      return (T)renderToString.call( Context.getCurrentContext(), scope, scope, args );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  @SuppressWarnings("unused")
  public static ScriptableObject init( String programName )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( loadSrcForName( programName, JavascriptTypeManifold.JS ) ) );
    Node programNode = parser.parse();
    Context.getCurrentContext().evaluateString( scope, programNode.genCode(), programName, 1, null );
    return scope;
  }

  private static ThreadLocal<Integer> _counter = ThreadLocal.withInitial( () -> 0 );

  @SuppressWarnings("unused")
  public static ScriptableObject initDirect( String source, String url )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( source, url ) );
    Node programNode = parser.parse();
    Context.getCurrentContext().evaluateString( scope, programNode.genCode(), "direct_" + _counter.get(), 1, null );
    _counter.set( _counter.get() + 1 );
    return scope;
  }

  @SuppressWarnings("unused")
  protected static Object evaluate( String source, String url )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( source, url ) );
    Node programNode = parser.parse();
    return Context.getCurrentContext().evaluateString( scope, programNode.genCode(), "evaluate_js", 1, null );
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
      System.err.println( "===\nWARNING: more than one ." + fileExt + " file corresponds with type: '" + fqn + "':\n" );
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

  private static Object absoluteOffset( int offset, IFile file )
  {
    if( file instanceof IFileFragment )
    {
      offset += ((IFileFragment)file).getOffset();
    }
    return offset;
  }
}
