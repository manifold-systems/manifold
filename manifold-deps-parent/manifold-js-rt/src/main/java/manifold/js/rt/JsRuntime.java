/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.js.rt;

import manifold.js.rt.parser.Parser;
import manifold.js.rt.parser.TemplateParser;
import manifold.js.rt.parser.TemplateTokenizer;
import manifold.js.rt.parser.Tokenizer;
import manifold.js.rt.parser.tree.ClassNode;
import manifold.js.rt.parser.tree.Node;
import manifold.js.rt.parser.tree.template.JSTNode;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JsRuntime
{
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
  public static ScriptableObject init( String fqn, String content, String url )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( content, url ) );
    Node programNode = parser.parse();
    ClassNode classNode = programNode.getFirstChild( ClassNode.class );
    String script = classNode.genCode();
    Context.getCurrentContext().evaluateString( scope, script, fqn, 1, null );
    return scope;
  }

  @SuppressWarnings("unused")
  public static ScriptableObject initInstance( ScriptableObject scope, String name, Object... args )
  {
    return (ScriptableObject)Context.getCurrentContext().newObject( scope, name, args );
  }


  //// JavascriptProgram

  @SuppressWarnings("unused")
  public static <T> T invokeProg( ScriptableObject scope, String func, Object... args )
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
  public static ScriptableObject initProg( String fqn, String source, String url )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( source, url ) );
    Node programNode = parser.parse();
    Context.getCurrentContext().evaluateString( scope, programNode.genCode(), fqn, 1, null );
    return scope;
  }

  private static ThreadLocal<Integer> _programCounter = ThreadLocal.withInitial( () -> 0 );

  @SuppressWarnings("unused")
  public static ScriptableObject initDirect( String source, String url )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( source, url ) );
    Node programNode = parser.parse();
    Context.getCurrentContext().evaluateString( scope, programNode.genCode(), "direct_" + _programCounter.get(), 1, null );
    _programCounter.set( _programCounter.get() + 1 );
    return scope;
  }

  @SuppressWarnings("unused")
  public static Object evaluate( String source, String url )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    Parser parser = new Parser( new Tokenizer( source, url ) );
    Node programNode = parser.parse();
    return Context.getCurrentContext().evaluateString( scope, programNode.genCode(), "evaluate_js", 1, null );
  }

  //// JavascriptTemplate

  private static ThreadLocal<Integer> _Templatecounter = ThreadLocal.withInitial( () -> 0 );

  @SuppressWarnings("unused")
  public static ScriptableObject initEngine( JSTNode templateNode )
  {
    ScriptableObject scope = SharedScope.newStaticScope();
    String name = "template_" + _Templatecounter.get();
    _Templatecounter.set( _Templatecounter.get() + 1 );
    Context.getCurrentContext().evaluateString( scope, templateNode.genCode(), name, 1, null );
    return scope;
  }

  @SuppressWarnings("unused")
  public static JSTNode initNode( String fqn, String source, String url )
  {
    TemplateParser parser = new TemplateParser(
      new TemplateTokenizer( fqn, source, url, true ) );
    return (JSTNode)parser.parse();
  }
}
