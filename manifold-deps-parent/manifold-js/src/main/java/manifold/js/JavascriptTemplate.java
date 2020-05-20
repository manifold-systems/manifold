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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import manifold.api.DisableStringLiteralTemplates;
import manifold.api.fs.IFile;
import manifold.api.gen.*;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.js.rt.JsRuntime;
import manifold.js.rt.parser.tree.template.JSTNode;
import manifold.js.rt.parser.tree.template.RawStringNode;
import manifold.rt.api.util.ManEscapeUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import static manifold.js.JavascriptProgram.*;

public class JavascriptTemplate
{
  static SrcClass genClass( String fqn, JSTNode jstNode )
  {
    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class )
      .addImport( JsRuntime.class );

    IFile file = loadSrcForName( fqn, JavascriptTypeManifold.JS );
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

    clazz.addField( new SrcField( "TEMPLATE_NODE", JSTNode.class )
      .addAnnotation( new SrcAnnotationExpression( DisableStringLiteralTemplates.class ) )
      .modifiers( Modifier.STATIC | Modifier.FINAL )
      .initializer( JsRuntime.class.getSimpleName() + ".initNode(\"" + fqn + "\",\"" + source + "\",\"" + url + "\")" ) );

    clazz.addField( new SrcField( "SCOPE", ScriptableObject.class )
      .modifiers( Modifier.STATIC | Modifier.FINAL )
      .initializer( JsRuntime.class.getSimpleName() + ".initEngine(TEMPLATE_NODE)" ) );

    AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
      .name( "renderToString" )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .returns( String.class );

    List<SrcParameter> srcParameters = makeSrcParameters( jstNode, srcMethod );
    srcMethod.body( "return " + JavascriptTemplate.class.getTypeName() +
                    ".renderToStringImpl(SCOPE, TEMPLATE_NODE" + generateArgList( srcParameters ) + ");" );
    clazz.addMethod( srcMethod );

    return clazz;
  }

  //Calls the generated renderToString function with raw strings from template
  @SuppressWarnings("unused")
  public static String renderToStringImpl( ScriptableObject scope, JSTNode templateNode, Object... args )
  {
    try
    {
      //make argument list including the raw string list
      Object[] argsWithStrings = Arrays.copyOf( args, args.length + 1 );

      List rawStrings = templateNode.getChildren( RawStringNode.class )
        .stream()
        .map( node -> node.genCode() )
        .collect( Collectors.toList() );

      argsWithStrings[argsWithStrings.length - 1] = rawStrings;

      Function renderToString = (Function)scope.get( "renderToString", scope );
      return (String)renderToString.call( Context.getCurrentContext(), scope, scope, argsWithStrings );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
