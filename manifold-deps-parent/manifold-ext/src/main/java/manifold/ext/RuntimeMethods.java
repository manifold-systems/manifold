package manifold.ext;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.lang.model.type.NoType;
import manifold.ext.api.ICallHandler;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavaParser;
import manifold.util.Pair;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.ConcurrentWeakHashMap;

public class RuntimeMethods
{
  private static final String STRUCTURAL_PROXY = "_structuralproxy_";
  private static Map<Class, Map<Class, Constructor>> PROXY_CACHE = new ConcurrentHashMap<>();
  private static final Map<Object, Set<Class>> ID_MAP = new ConcurrentWeakHashMap<>();

  @SuppressWarnings("UnusedDeclaration")
  public static Object constructProxy( Object root, Class iface )
  {
    // return findCachedProxy( root, iface ); // this is only beneficial when structural invocation happens in a loop, otherwise too costly
    return createNewProxy( root, iface );
  }

  @SuppressWarnings("UnusedDeclaration")
  public static Object assignStructuralIdentity( Object obj, Class iface )
  {
    if( obj != null )
    {
      //## note: we'd like to avoid the operation if the obj not a ICallHandler,
      // but that is an expensive structural check, more expensive than this call...
      //  if( obj is a ICallHandler )
      //  {
      Set<Class> ifaces = ID_MAP.computeIfAbsent( obj, k -> new ConcurrentHashSet<>() );
      ifaces.add( iface );
      //   }
    }
    return obj;
  }

  /**
   * Facilitates ICallHandler where the receiver of the method call structurally implements a method,
   * but the association of the structural interface with the receiver is lost.  For example:
   * <pre>
   *   Person person = Person.create(); // Person is a JsonTypeManifold interface; the runtime type of person here is really just a Map (or Binding)
   *   IMyStructureThing thing = (IMyStructureThing)person; // Extension method[s] satisfying IMyStructureThing on Person make this work e.g., via MyPersonExt extension methods class
   *   thing.foo(); // foo() is an extension method on Person e.g., defined in MyPersonExt, however the runtime type of thing is just a Map (or Binding) thus the Person type identity is lost
   * </pre>
   */
  //## todo: this is inefficient, we should consider caching the methods by signature along with the interfaces
  public static Object invokeUnhandled( Object thiz, Class proxiedIface, String name, Class returnType, Class[] paramTypes, Object[] args )
  {
    Set<Class> ifaces = ID_MAP.get( thiz );
    if( ifaces != null )
    {
      for( Class iface : ifaces )
      {
        if( iface == proxiedIface )
        {
          continue;
        }

        Method m = findMethod( iface, name, paramTypes );
        if( m != null )
        {
          try
          {
            Object result = m.invoke( constructProxy( thiz, iface ), args );
            //## todo: maybe coerce result if return types are not directly assignable?  e.g., Integer vs. Double
            return result;
          }
          catch( Exception e )
          {
            throw new RuntimeException( e );
          }
        }
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Method findMethod( Class<?> iface, String name, Class[] paramTypes )
  {
    try
    {
      Method m = iface.getDeclaredMethod( name, paramTypes );
      if( m == null )
      {
        for( Class superIface : iface.getInterfaces() )
        {
          m = findMethod( superIface, name, paramTypes );
          if( m != null )
          {
            break;
          }
        }
      }
      if( m != null )
      {
        return m;
      }
    }
    catch( Exception e )
    {
      return null;
    }
    return null;
  }

  private static Object createNewProxy( Object root, Class<?> iface )
  {
    if( root == null )
    {
      return null;
    }

    Class rootClass = root.getClass();
    if( iface.isAssignableFrom( rootClass ) )
    {
      return root;
    }

//    final Field classRedefinedCount;
//    try
//    {
//      classRedefinedCount = Class.class.getDeclaredField( "classRedefinedCount" );
//      classRedefinedCount.setAccessible( true );
//      System.out.println( "### " + iface.getSimpleName() + ": " + classRedefinedCount.getInt( iface ) );
//    }
//    catch( Exception e )
//    {
//      throw new RuntimeException( e );
//    }

    Map<Class, Constructor> proxyByClass = PROXY_CACHE.get( iface );
    if( proxyByClass == null )
    {
      PROXY_CACHE.put( iface, proxyByClass = new ConcurrentHashMap<>() );
    }
    Constructor proxyClassCtor = proxyByClass.get( rootClass );
    if( proxyClassCtor == null ) //|| BytecodeOptions.JDWP_ENABLED.get() )
    {
      Class proxyClass = createProxy( iface, rootClass );
      proxyByClass.put( rootClass, proxyClassCtor = proxyClass.getConstructors()[0] );
    }
    try
    {
      // in Java 9 in modular mode the proxy class belongs to the owner's module,
      // therefore we need to make it accessible from the manifold module before
      // calling newInstance()
      ReflectUtil.setAccessible( proxyClassCtor );
      return proxyClassCtor.newInstance( root );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private static Class createProxy( Class iface, Class rootClass )
  {
    String relativeProxyName = rootClass.getCanonicalName().replace( '.', '_' ) + STRUCTURAL_PROXY + iface.getCanonicalName().replace( '.', '_' );
    if( hasCallHandlerMethod( rootClass ) )
    {
      return DynamicTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
    }
    return StructuralTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
  }

  private static boolean hasCallHandlerMethod( Class rootClass )
  {
    String fqn = rootClass.getCanonicalName();
    BasicJavacTask javacTask = JavaParser.instance().getJavacTask();
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( ManifoldHost.getGlobalModule() ).getClassSymbol( javacTask, fqn );
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> callHandlerSymbol = ClassSymbols.instance( ManifoldHost.getGlobalModule() ).getClassSymbol( javacTask, ICallHandler.class.getCanonicalName() );
    if( Types.instance( javacTask.getContext() ).isAssignable( classSymbol.getFirst().asType(), callHandlerSymbol.getFirst().asType() ) )
    {
      // Nominally implements ICallHandler
      return true;
    }

    return hasCallMethod( javacTask, classSymbol.getFirst() );
  }

  private static boolean hasCallMethod( BasicJavacTask javacTask, Symbol.ClassSymbol classSymbol )
  {
    Name call = Names.instance( javacTask.getContext() ).fromString( "call" );
    Iterable<Symbol> elems = IDynamicJdk.instance().getMembersByName( classSymbol, call );
    for( Symbol s : elems )
    {
      if( s instanceof Symbol.MethodSymbol )
      {
        List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)s).getParameters();
        if( parameters.size() != 6 )
        {
          return false;
        }

        Symtab symbols = Symtab.instance( javacTask.getContext() );
        Types types = Types.instance( javacTask.getContext() );
        return types.erasure( parameters.get( 0 ).asType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 1 ).asType().equals( symbols.stringType ) &&
               parameters.get( 2 ).asType().equals( symbols.stringType ) &&
               types.erasure( parameters.get( 3 ).asType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 4 ).asType() instanceof Type.ArrayType && types.erasure( ((Type.ArrayType)parameters.get( 4 ).asType()).getComponentType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 5 ).asType() instanceof Type.ArrayType && ((Type.ArrayType)parameters.get( 5 ).asType()).getComponentType().equals( symbols.objectType );
      }
    }
    Type superclass = classSymbol.getSuperclass();
    if( !(superclass instanceof NoType) )
    {
      if( hasCallMethod( javacTask, (Symbol.ClassSymbol)superclass.tsym ) )
      {
        return true;
      }
    }
    for( Type iface : classSymbol.getInterfaces() )
    {
      if( hasCallMethod( javacTask, (Symbol.ClassSymbol)iface.tsym ) )
      {
        return true;
      }
    }
    return false;
  }


}
