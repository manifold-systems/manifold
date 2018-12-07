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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.lang.model.type.NoType;
import manifold.ext.api.ICallHandler;
import manifold.internal.host.RuntimeManifoldHost;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.IDynamicJdk;
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
            result = coerce( result, returnType );
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

  public static Object coerce( Object value, Class<?> type )
  {
    if( value == null )
    {
      return null;
    }

    if( type.isPrimitive() )
    {
      type = box( type );
    }

    Class<?> valueClass = value.getClass();
    if( valueClass == type || type.isAssignableFrom( valueClass ) )
    {
      return value;
    }

    if( type == Boolean.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).intValue() != 0;
      }
      return Boolean.parseBoolean( value.toString() );
    }

    if( type == Byte.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).byteValue() != 0;
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? (byte)1: (byte)0;
      }
      return Byte.parseByte( value.toString() );
    }

    if( type == Character.class )
    {
      if( value instanceof Number )
      {
        return (char)((Number)value).intValue();
      }
      String s = value.toString();
      return s.isEmpty() ? (char)0 : s.charAt( 0 );
    }

    if( type == Short.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).shortValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? (short)1: (short)0;
      }
      return Short.parseShort( value.toString() );
    }

    if( type == Integer.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).intValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1: 0;
      }
      return Integer.parseInt( value.toString() );
    }

    if( type == Long.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).longValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1L: 0L;
      }
      return Long.parseLong( value.toString() );
    }

    if( type == Float.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).floatValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1f: 0f;
      }
      return Float.parseFloat( value.toString() );
    }

    if( type == Double.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).doubleValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1d : 0d;
      }
      return Double.parseDouble( value.toString() );
    }

    if( type == BigInteger.class )
    {
      if( value instanceof Number )
      {
        return BigInteger.valueOf( ((Number)value).longValue() );
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? BigInteger.ONE: BigInteger.ZERO;
      }
      return new BigInteger( value.toString() );
    }

    if( type == BigDecimal.class )
    {
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? BigDecimal.ONE: BigDecimal.ZERO;
      }
      return new BigDecimal( value.toString() );
    }

    if( type == String.class )
    {
      return String.valueOf( value );
    }

    if( type.isArray() && valueClass.isArray() )
    {
      int length = Array.getLength( value );
      Class<?> componentType = type.getComponentType();
      Object array = Array.newInstance( componentType, length );
      for( int i = 0; i < length; i++ )
      {
        Array.set( array, i, coerce( Array.get( value, i ), componentType ) );
      }
      return array;
    }

    // oh well, let the ClassCastException loose
    return value;
  }

  private static Class<?> box( Class<?> type )
  {
    if( type == boolean.class )
    {
      return Boolean.class;
    }
    if( type == byte.class )
    {
      return Byte.class;
    }
    if( type == char.class )
    {
      return Character.class;
    }
    if( type == short.class )
    {
      return Short.class;
    }
    if( type == int.class )
    {
      return Integer.class;
    }
    if( type == long.class )
    {
      return Long.class;
    }
    if( type == float.class )
    {
      return Float.class;
    }
    if( type == double.class )
    {
      return Double.class;
    }
    throw new IllegalStateException();
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
    BasicJavacTask javacTask = RuntimeManifoldHost.get().getJavaParser().getJavacTask();
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( RuntimeManifoldHost.get().getSingleModule() ).getClassSymbol( javacTask, fqn );
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> callHandlerSymbol = ClassSymbols.instance( RuntimeManifoldHost.get().getSingleModule() ).getClassSymbol( javacTask, ICallHandler.class.getCanonicalName() );
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

  public static Object invoke_Object( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return ReflectUtil.method( receiver, name, paramTypes ).invoke( args );
  }
  public static boolean invoke_boolean( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (boolean)invoke_Object( receiver, name, paramTypes, args );
  }
  public static byte invoke_byte( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (byte)invoke_Object( receiver, name, paramTypes, args );
  }
  public static char invoke_char( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (char)invoke_Object( receiver, name, paramTypes, args );
  }
  public static int invoke_int( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (int)invoke_Object( receiver, name, paramTypes, args );
  }
  public static long invoke_long( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (long)invoke_Object( receiver, name, paramTypes, args );
  }
  public static float invoke_float( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (float)invoke_Object( receiver, name, paramTypes, args );
  }
  public static double invoke_double( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (double)invoke_Object( receiver, name, paramTypes, args );
  }
  public static void invoke_void( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    invoke_Object( receiver, name, paramTypes, args );
  }

  public static Object invokeStatic_Object( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return ReflectUtil.method( cls, name, paramTypes ).invokeStatic( args );
  }
  public static boolean invokeStatic_boolean( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (boolean)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static byte invokeStatic_byte( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (byte)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static char invokeStatic_char( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (char)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static int invokeStatic_int( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (int)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static long invokeStatic_long( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (long)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static float invokeStatic_float( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (float)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static double invokeStatic_double( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (double)invokeStatic_Object( cls, name, paramTypes, args );
  }
  public static void invokeStatic_void( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static Object getField_Object( Object receiver, String name )
  {
    return ReflectUtil.field( receiver, name ).get();
  }
  public static boolean getField_boolean( Object receiver, String name )
  {
    return (boolean)getField_Object( receiver, name );
  }
  public static byte getField_byte( Object receiver, String name )
  {
    return (byte)getField_Object( receiver, name );
  }
  public static char getField_char( Object receiver, String name )
  {
    return (char)getField_Object( receiver, name );
  }
  public static int getField_int( Object receiver, String name )
  {
    return (int)getField_Object( receiver, name );
  }
  public static long getField_long( Object receiver, String name )
  {
    return (long)getField_Object( receiver, name );
  }
  public static float getField_float( Object receiver, String name )
  {
    return (float)getField_Object( receiver, name );
  }
  public static double getField_double( Object receiver, String name )
  {
    return (double)getField_Object( receiver, name );
  }
  
  public static Object getFieldStatic_Object( Class receiver, String name )
  {
    return ReflectUtil.field( receiver, name ).getStatic();
  }
  public static boolean getFieldStatic_boolean( Class receiver, String name )
  {
    return (boolean)getFieldStatic_Object( receiver, name );
  }
  public static byte getFieldStatic_byte( Class receiver, String name )
  {
    return (byte)getFieldStatic_Object( receiver, name );
  }
  public static char getFieldStatic_char( Class receiver, String name )
  {
    return (char)getFieldStatic_Object( receiver, name );
  }
  public static int getFieldStatic_int( Class receiver, String name )
  {
    return (int)getFieldStatic_Object( receiver, name );
  }
  public static long getFieldStatic_long( Class receiver, String name )
  {
    return (long)getFieldStatic_Object( receiver, name );
  }
  public static float getFieldStatic_float( Class receiver, String name )
  {
    return (float)getFieldStatic_Object( receiver, name );
  }
  public static double getFieldStatic_double( Class receiver, String name )
  {
    return (double)getFieldStatic_Object( receiver, name );
  }

  public static void setField_Object( Object receiver, String name, Object value )
  {
    ReflectUtil.field( receiver, name ).set( value );
  }
  public static void setField_boolean( Object receiver, String name, boolean value )
  {
    setField_Object( receiver, name, value );
  }
  public static void setField_byte( Object receiver, String name, byte value )
  {
    setField_Object( receiver, name, value );
  }
  public static void setField_char( Object receiver, String name, char value )
  {
    setField_Object( receiver, name, value );
  }
  public static void setField_int( Object receiver, String name, int value )
  {
    setField_Object( receiver, name, value );
  }
  public static void setField_long( Object receiver, String name, long value )
  {
    setField_Object( receiver, name, value );
  }
  public static void setField_float( Object receiver, String name, float value )
  {
    setField_Object( receiver, name, value );
  }
  public static void setField_double( Object receiver, String name, double value )
  {
    setField_Object( receiver, name, value );
  }
  
  public static void setFieldStatic_Object( Class receiver, String name, Object value )
  {
    ReflectUtil.field( receiver, name ).setStatic( value );
  }
  public static void setFieldStatic_boolean( Class receiver, String name, boolean value )
  {
    setFieldStatic_Object( receiver, name, value );
  }
  public static void setFieldStatic_byte( Class receiver, String name, byte value )
  {
    setFieldStatic_Object( receiver, name, value );
  }
  public static void setFieldStatic_char( Class receiver, String name, char value )
  {
    setFieldStatic_Object( receiver, name, value );
  }
  public static void setFieldStatic_int( Class receiver, String name, int value )
  {
    setFieldStatic_Object( receiver, name, value );
  }
  public static void setFieldStatic_long( Class receiver, String name, long value )
  {
    setFieldStatic_Object( receiver, name, value );
  }
  public static void setFieldStatic_float( Class receiver, String name, float value )
  {
    setFieldStatic_Object( receiver, name, value );
  }
  public static void setFieldStatic_double( Class receiver, String name, double value )
  {
    setFieldStatic_Object( receiver, name, value );
  }

  public static Object construct( Class type, Class[] paramTypes, Object[] args )
  {
    return ReflectUtil.constructor( type, paramTypes ).newInstance( args );
  }
}
