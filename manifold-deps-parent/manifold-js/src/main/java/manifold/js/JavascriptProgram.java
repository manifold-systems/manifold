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

import manifold.api.DisableStringLiteralTemplates;
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
import manifold.api.type.ResourceFileTypeManifold;
import manifold.api.type.SourcePosition;
import manifold.internal.javac.JavacPlugin;
import manifold.js.rt.JsRuntime;
import manifold.js.rt.parser.tree.*;
import manifold.rt.api.util.ManEscapeUtil;
import org.mozilla.javascript.ScriptableObject;


public class JavascriptProgram
{
  static SrcClass genProgram( String fqn, ProgramNode programNode, IFile file )
  {
    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class ).superClass( JavascriptProgram.class )
      .imports( JsRuntime.class )
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
        .initializer( JsRuntime.class.getSimpleName() + ".initDirect(\"" + ManEscapeUtil.escapeForJava(
          ((FileFragmentImpl)file).getContent() ) + "\", \"" + url + "\")" ) );

      AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
        .name( "fragmentValue" )
        .modifiers( Modifier.STATIC | Modifier.PUBLIC )
        .returns( Object.class.getSimpleName() );
      srcMethod.body( "return " + JsRuntime.class.getSimpleName() + ".evaluate(\"" + ManEscapeUtil.escapeForJava(
        ((FileFragmentImpl)file).getContent() ) + "\", \"" + url + "\");" );
      clazz.addMethod( srcMethod );

    }
    else
    {
      String url;
      try
      {
        url = file.toURI().toURL().toString();
      }
      catch( MalformedURLException e )
      {
        throw new RuntimeException( e );
      }
      String source = ResourceFileTypeManifold.getContent( file );
      source = ManEscapeUtil.escapeForJavaStringLiteral( source );

      clazz.addField( new SrcField( "SCOPE", ScriptableObject.class )
        .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class ) )
        .modifiers( Modifier.STATIC )
        .initializer( JsRuntime.class.getSimpleName() + ".initProg(\"" + fqn + "\",\"" + source + "\",\"" + url + "\")" ) );
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
        .body( "return " + JsRuntime.class.getSimpleName() + ".invokeProg(SCOPE, \"" + node.getName() + "\"" +
               generateArgList( makeSrcParameters( node, srcMethod ) ) + ");" );
      clazz.addMethod( srcMethod );
    }
    return clazz;
  }

  static List<SrcParameter> makeSrcParameters( Node node, SrcAnnotated srcMethod )
  {
    ParameterNode paramNode = node.getFirstChild( ParameterNode.class );
    List<SrcParameter> srcParameters = paramNode != null
      ? JavascriptClass.toParamList( paramNode )
      : Collections.emptyList();
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
    ITypeManifold tm = JavacPlugin.instance().getHost().getSingleModule().getTypeManifolds().stream()
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
