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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import manifold.api.gen.AbstractSrcMethod;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.js.parser.TemplateParser;
import manifold.js.parser.TemplateTokenizer;
import manifold.js.parser.tree.template.JSTNode;
import manifold.js.parser.tree.template.RawStringNode;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;


import static manifold.js.JavascriptProgram.generateArgList;
import static manifold.js.JavascriptProgram.makeSrcParameters;
import static manifold.js.Util.safe;

public class JavascriptTemplate
{

  public static SrcClass genClass( String fqn, JSTNode jstNode )
  {
    SrcClass clazz = new SrcClass( fqn, SrcClass.Kind.Class );

    clazz.addField( new SrcField( "TEMPLATE_NODE", JSTNode.class )
      .modifiers( Modifier.STATIC )
      .initializer( new SrcRawExpression( ("manifold.js.JavascriptTemplate.initNode(\"" + fqn + "\")") ) ) );

    clazz.addField( new SrcField( "ENGINE", ScriptableObject.class )
      .modifiers( Modifier.STATIC )
      .initializer( new SrcRawExpression( ("manifold.js.JavascriptTemplate.initEngine(TEMPLATE_NODE)") ) ) );

    AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
      .name( "renderToString" )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC )
      .returns( String.class );

    List<SrcParameter> srcParameters = makeSrcParameters( jstNode, srcMethod );
    srcMethod.body( new SrcStatementBlock()
      .addStatement(
        new SrcRawStatement()
          .rawText( "return manifold.js.JavascriptTemplate.renderToStringImpl(ENGINE, TEMPLATE_NODE" + generateArgList( srcParameters ) + ");" ) ) );
    clazz.addMethod( srcMethod );

    return clazz;
  }

  //Calls the generated renderToString function with raw strings from template
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

  private static ThreadLocal<Integer> _counter = ThreadLocal.withInitial( () -> 0 );

  public static ScriptableObject initEngine( JSTNode templateNode )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    String name = "template_" + _counter.get();
    _counter.set( _counter.get() + 1 );
    safe( () -> Context.getCurrentContext().evaluateString( scope, templateNode.genCode(), name, 1, null ) );
    return scope;
  }

  public static JSTNode initNode( String programName )
  {
    TemplateParser parser = new TemplateParser( new TemplateTokenizer( JavascriptProgram.loadSrcForName( programName, JavascriptTypeManifold.JST ), true ) );
    return (JSTNode)parser.parse();
  }
}
