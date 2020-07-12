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

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.DeferredAttr;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Flow;
import com.sun.tools.javac.comp.Infer;
import com.sun.tools.javac.comp.LambdaToMethod;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.jvm.Gen;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import javax.tools.JavaFileObject;

import manifold.util.JreUtil;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

public class ManResolve extends Resolve
{
  private static final String RESOLVE_FIELD = "rs";

  private static final LocklessLazyVar<Class<?>> EXTENSION_TRANSFORMER = LocklessLazyVar.make(
    () -> ReflectUtil.type( "manifold.ext.ExtensionTransformer" )
  );

  private final Attr _attr;

  public static Resolve instance( Context ctx )
  {
    Resolve resolve = ctx.get( resolveKey );
    if( !(resolve instanceof ManResolve) )
    {
      ctx.put( resolveKey, (Resolve)null );
      resolve = new ManResolve( ctx );
    }

    return resolve;
  }

  @SuppressWarnings("ConstantConditions")
  private ManResolve( Context context )
  {
    super( context );
    _attr = Attr.instance( context );
    ReflectUtil.field( this, "log" ).set( ReflectUtil.field( _attr, "log" ).get() );

    if( JreUtil.isJava8() )
    {
      reassignEarlyHolders8( context );
    }
    else
    {
      reassignEarlyHolders( context );
    }
  }

  private void reassignEarlyHolders8( Context context )
  {
    ReflectUtil.field( Attr.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( DeferredAttr.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Check.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Infer.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Flow.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Lower.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Gen.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Annotate.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( JavacTrees.instance( context ), "resolve" ).set( this );
    ReflectUtil.field( TransTypes.instance( context ), "resolve" ).set( this );
  }

  private void reassignEarlyHolders( Context context )
  {
    ReflectUtil.field( _attr, RESOLVE_FIELD ).set( this );
    ReflectUtil.field( DeferredAttr.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Check.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Infer.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Flow.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( LambdaToMethod.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Lower.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field( Gen.instance( context ), RESOLVE_FIELD ).set( this );
    ReflectUtil.field(
      ReflectUtil.method(
        ReflectUtil.type( "com.sun.tools.javac.jvm.StringConcat" ), "instance", Context.class )
        .invokeStatic( context ), RESOLVE_FIELD )
      .set( this );
    ReflectUtil.field( JavacTrees.instance( context ), "resolve" ).set( this );
    ReflectUtil.field( Annotate.instance( context ), "resolve" ).set( this );
    ReflectUtil.field( TransTypes.instance( context ), "resolve" ).set( this );
    ReflectUtil.field( JavacElements.instance( context ), "resolve" ).set( this );
  }

  /**
   * Allow augmented classes to access modules as if defined in both the extended class' module and
   * the extension class' module.
   */
  @Override
  public boolean isAccessible( Env<AttrContext> env, Symbol.TypeSymbol typeSymbol, boolean checkInner )
  {
    boolean accessible = super.isAccessible( env, typeSymbol, checkInner );
    if( accessible )
    {
      return true;
    }

    if( isJailbreakOnType() )
    {
      // handle the case where the class itself is inaccessible:
      //
      // // the *type* must be @Jailbreak as well as the constructor
      // com.foo.@Jailbreak PrivateClass privateThing = new com.foo.@Jailbreak PrivateClass();
      // privateThing.privateMethod();
      // ...
      return true;
    }

    if( JreUtil.isJava8() )
    {
      return false;
    }


    // Java 9 +

    JavaFileObject sourceFile = env.toplevel.getSourceFile();
    if( sourceFile instanceof GeneratedJavaStubFileObject )
    {
      // Allow augmented classes to access modules as if defined in both the extended class' module and
      // the extension class' module.
      accessible = true;
    }

    return accessible;
  }

  private boolean isJailbreakOnType()
  {
    JCTree.JCAnnotatedType annotatedType = ((ManAttr)_attr).peekAnnotatedType();
    if( annotatedType != null )
    {
      return annotatedType.toString().contains( "@Jailbreak" );
    }
    return false;
  }

  /**
   * Allow @Jailbreak to expose otherwise inaccessible features
   */
  @Override
  public boolean isAccessible( Env<AttrContext> env, Type site, Symbol sym, boolean checkInner )
  {
    boolean accessible = super.isAccessible( env, site, sym, checkInner );
    if( accessible )
    {
      return true;
    }

    if( isJailbreak( sym ) )
    {
      return true;
    }
    return isJailbreak( env.tree );
  }

  private boolean isJailbreak( Symbol sym )
  {
    Class<?> extensionTransformer = EXTENSION_TRANSFORMER.get();
    if( extensionTransformer == null )
    {
      return false;
    }

    return (boolean)ReflectUtil.method( extensionTransformer, "isJailbreakSymbol", Symbol.class )
      .invokeStatic( sym );
  }

  private boolean isJailbreak( JCTree tree )
  {
    if( !(tree instanceof JCTree.JCMethodInvocation) &&
        !(tree instanceof JCTree.JCFieldAccess) &&
        !(tree instanceof JCTree.JCAssign) &&
        !(tree instanceof JCTree.JCNewClass) &&
        !(tree instanceof JCTree.JCVariableDecl) &&
        ((ManAttr)_attr).peekSelect() == null )
    {
      return false;
    }

    Class<?> extensionTransformer = EXTENSION_TRANSFORMER.get();
    if( extensionTransformer == null )
    {
      return false;
    }

    boolean isJailbreak = (boolean)ReflectUtil.method( extensionTransformer, "isJailbreakReceiver", JCTree.class )
      .invokeStatic( tree );
    if( !isJailbreak )
    {
      JCTree.JCFieldAccess select = ((ManAttr)_attr).peekSelect();
      if( select != null && select != tree )
      {
        isJailbreak = (boolean)ReflectUtil.method( extensionTransformer, "isJailbreakReceiver", JCTree.JCFieldAccess.class )
          .invokeStatic( select );
      }
    }
    return isJailbreak;
  }
}
