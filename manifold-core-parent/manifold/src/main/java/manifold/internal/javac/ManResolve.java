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
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

public class ManResolve extends Resolve
{
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

    if( ! JavacPlugin.IS_JAVA_8 )
    {
      reassignEarlyHolders( context );
    }
  }

  private void reassignEarlyHolders( Context context )
  {
    ReflectUtil.field( _attr, "rs" ).set( this );
    ReflectUtil.field( DeferredAttr.instance( context ), "rs" ).set( this );
    ReflectUtil.field( Check.instance( context ), "rs" ).set( this );
    ReflectUtil.field( Infer.instance( context ), "rs" ).set( this );
    ReflectUtil.field( Flow.instance( context ), "rs" ).set( this );
    ReflectUtil.field( LambdaToMethod.instance( context ), "rs" ).set( this );
    ReflectUtil.field( Lower.instance( context ), "rs" ).set( this );
    ReflectUtil.field( Gen.instance( context ), "rs" ).set( this );
    ReflectUtil.field(
      ReflectUtil.method(
        ReflectUtil.type( "com.sun.tools.javac.jvm.StringConcat" ), "instance", Context.class )
        .invokeStatic( context ), "rs" )
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

    if( isJailBreakOnType() )
    {
      // handle the case where the class itself is inaccessible:
      //
      // // the *type* must be @JailBreak as well as the constructor
      // com.foo.@JailBreak PrivateClass privateThing = new com.foo.@JailBreak PrivateClass();
      // privateThing.privateMethod();
      // ...
      return true;
    }

    if( JavacPlugin.IS_JAVA_8 )
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

  private boolean isJailBreakOnType()
  {
    JCTree.JCAnnotatedType annotatedType = ((ManAttr)_attr).peekAnnotatedType();
    if( annotatedType != null )
    {
      return annotatedType.toString().contains( "@JailBreak" );
    }
    return false;
  }

  /**
   * Allow @JailBreak to expose otherwise inaccessible features
   */
  @Override
  public boolean isAccessible( Env<AttrContext> env, Type site, Symbol sym, boolean checkInner )
  {
    boolean accessible = super.isAccessible( env, site, sym, checkInner );
    if( accessible )
    {
      return true;
    }

    if( isJailBreak( sym ) )
    {
      return true;
    }
    return isJailBreak( env.tree );
  }

  private boolean isJailBreak( Symbol sym )
  {
    Class<?> extensionTransformer = EXTENSION_TRANSFORMER.get();
    if( extensionTransformer == null )
    {
      return false;
    }

    return (boolean)ReflectUtil.method( extensionTransformer, "isJailBreakSymbol", Symbol.class )
      .invokeStatic( sym );
  }

  private boolean isJailBreak( JCTree tree )
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

    boolean isJailBreak = (boolean)ReflectUtil.method( extensionTransformer, "isJailBreakReceiver", JCTree.class )
      .invokeStatic( tree );
    if( !isJailBreak )
    {
      JCTree.JCFieldAccess select = ((ManAttr)_attr).peekSelect();
      if( select != null && select != tree )
      {
        isJailBreak = (boolean)ReflectUtil.method( extensionTransformer, "isJailBreakReceiver", JCTree.JCFieldAccess.class )
          .invokeStatic( select );
      }
    }
    return isJailBreak;
  }
}
