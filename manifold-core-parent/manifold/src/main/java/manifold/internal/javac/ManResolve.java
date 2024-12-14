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
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
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
    ReflectUtil.field( LambdaToMethod.instance( context ), RESOLVE_FIELD ).set( this );
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

    if( JreUtil.isJava11orLater() )
    {
      // Allow @var to work with properties.
      // Note, this is not as scary as it looks. Setting allowLocalVariableTypeInference to false only turns off
      // unnecessary name checking so we can use @var annotation type, which should be allowed because `@` effectively
      // escapes the name, so there really isn't any conflict with Java's 'var' construct. Just sayin'
      ReflectUtil.field( this, "allowLocalVariableTypeInference" ).set( false );
    }
    else if( JreUtil.isJava17orLater() )
    {
      ReflectUtil.field( ReflectUtil.method( "com.sun.tools.javac.comp.TransPattern", "instance", Context.class )
        .invokeStatic( context ), RESOLVE_FIELD ).set( this );
    }
  }

  public boolean isAccessible( Env<AttrContext> env, Type site, Symbol sym )
  {
    boolean accessible = isAccessible( env, site, sym, false );
    if( !accessible )
    {
      return false;
    }

    return disambiguatePropertyRef( site, sym );
  }

  // Here we disambiguate a reference to a property from the implementing class where the property field comes from both
  // the interface and the super class. The property should always resolve through the super class not the interface.
  private boolean disambiguatePropertyRef( Type site, Symbol sym )
  {
    if( sym instanceof Symbol.VarSymbol && sym.owner.isInterface() &&  // property sym is directly an interface member, not impl on a class
        site.tsym instanceof ClassSymbol && !site.tsym.isInterface() ) // site sym is a class, not an interface
    {
      ClassSymbol siteSym = (ClassSymbol)site.tsym;
      if( siteSym.getSuperclass() != null ) // site type has a superclass
      {
        Types types = Types.instance( JavacPlugin.instance().getContext() );
        if( types.isSubtype( siteSym.getSuperclass(), sym.owner.type ) ) // site type implements interface
        {
        if( !hasAnnotation( sym, "manifold.ext.props.rt.api.Static" ) &&
            (hasAnnotation( sym, "manifold.ext.props.rt.api.get" ) ||
             hasAnnotation( sym, "manifold.ext.props.rt.api.set" ) ||
             hasAnnotation( sym, "manifold.ext.props.rt.api.val" ) ||
             hasAnnotation( sym, "manifold.ext.props.rt.api.var" )) )
          {
            // should always access through super class, not interface
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean hasAnnotation( Symbol sym, String anno )
  {
    for( Attribute.Compound a : sym.getAnnotationMirrors() )
    {
      String fqn = a.type.tsym.getQualifiedName().toString();
      if( fqn.equals( anno ) )
      {
        return true;
      }
    }
    return false;
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
    if( env.enclClass == null && JreUtil.isJava20orLater() )
    {
      // this check is here to fix an NPE caused by Valhalla build for java20, happens during ExtensionTransformer (env.enclClass is null)
      // remove this if-stmt if/when valhalla fixes it
      return sym.owner == site.tsym || sym.name == null || !"<init>".equals( sym.name.toString() );
    }

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
