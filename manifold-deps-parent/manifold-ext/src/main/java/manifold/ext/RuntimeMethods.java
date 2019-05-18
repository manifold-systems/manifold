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

package manifold.ext;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.script.Bindings;
import manifold.ext.api.IBindingType;
import manifold.ext.api.IBindingsBacked;
import manifold.ext.api.ICallHandler;
import manifold.ext.api.ICoercionProvider;
import manifold.ext.api.IProxyFactory;
import manifold.ext.api.Structural;
import manifold.util.ReflectUtil;
import manifold.util.ServiceUtil;
import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.ConcurrentWeakHashMap;
import manifold.util.concurrent.LocklessLazyVar;

public class RuntimeMethods
{
  private static Map<Class, Map<Class, IProxyFactory<?,?>>> PROXY_CACHE = new ConcurrentHashMap<>();
  private static final Map<Object, Set<Class>> ID_MAP = new ConcurrentWeakHashMap<>();
  private static final LocklessLazyVar<Set<IProxyFactory>> _registeredProxyFactories =
    LocklessLazyVar.make( () -> {
      Set<IProxyFactory> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, IProxyFactory.class, RuntimeMethods.class.getClassLoader() );
      return registered;
    } );

  @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
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
      for( Class iface: ifaces )
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

  /**
   * Coerce the value from a JSON bindings value to more type-safe a Java value, using {@link ICoercionProvider}
   * where applicable. Note for List the {@code type} corresponds with the deepest component type of the list.
   */
  public static Object coerce( Object value, Class<?> type )
  {
    if( value == null )
    {
      if( type.isPrimitive() )
      {
        return defaultPrimitiveValue( type );
      }
      return null;
    }

    if( IBindingsBacked.class.isAssignableFrom( type ) )
    {
      return value;
    }

    if( value instanceof List )
    {
      Class<?> finalType = type;
      //noinspection unchecked
      return ((List)value).stream()
      .map( e -> coerce( e, finalType ) )
      .collect( Collectors.toList() );
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

    Object result = callCoercionProviders( value, type );
    if( result != ICallHandler.UNHANDLED )
    {
      return result;
    }

    Object boxedValue = coerceBoxed( value, type );
    if( boxedValue != null )
    {
      return boxedValue;
    }

    if( type == BigInteger.class )
    {
      if( value instanceof Number )
      {
        return BigInteger.valueOf( ((Number)value).longValue() );
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? BigInteger.ONE : BigInteger.ZERO;
      }
      return new BigInteger( value.toString() );
    }

    if( type == BigDecimal.class )
    {
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? BigDecimal.ONE : BigDecimal.ZERO;
      }
      return new BigDecimal( value.toString() );
    }

    if( type == String.class )
    {
      return String.valueOf( value );
    }

    if( type.isEnum() )
    {
      String name = String.valueOf( value );
      //noinspection unchecked
      return Enum.valueOf( (Class<Enum>)type, name );
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

    // let the ClassCastException happen
    return value;
  }

  private static Object defaultPrimitiveValue( Class<?> type )
  {
    if( type == int.class ||
        type == short.class )
    {
      return 0;
    }
    if( type == byte.class )
    {
      return (byte)0;
    }
    if( type == long.class )
    {
      return 0L;
    }
    if( type == float.class )
    {
      return 0f;
    }
    if( type == double.class )
    {
      return 0d;
    }
    if( type == boolean.class )
    {
      return false;
    }
    if( type == char.class )
    {
      return (char)0;
    }
    if( type == void.class )
    {
      return null;
    }
    throw new IllegalArgumentException( "Unsupported primitive type: " + type.getSimpleName() );
  }

  private static Object coerceBoxed( Object value, Class<?> type )
  {
    if( type == Boolean.class || type == boolean.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).intValue() != 0;
      }
      return Boolean.parseBoolean( value.toString() );
    }

    if( type == Byte.class || type == byte.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).byteValue() != 0;
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? (byte)1 : (byte)0;
      }
      return Byte.parseByte( value.toString() );
    }

    if( type == Character.class || type == char.class )
    {
      if( value instanceof Number )
      {
        return (char)((Number)value).intValue();
      }
      String s = value.toString();
      return s.isEmpty() ? (char)0 : s.charAt( 0 );
    }

    if( type == Short.class || type == short.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).shortValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? (short)1 : (short)0;
      }
      return Short.parseShort( value.toString() );
    }

    if( type == Integer.class || type == int.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).intValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1 : 0;
      }
      return Integer.parseInt( value.toString() );
    }

    if( type == Long.class || type == long.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).longValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1L : 0L;
      }
      return Long.parseLong( value.toString() );
    }

    if( type == Float.class || type == float.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).floatValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1f : 0f;
      }
      return Float.parseFloat( value.toString() );
    }

    if( type == Double.class || type == double.class )
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
    return null;
  }

  private static Object callCoercionProviders( Object value, Class<?> type )
  {
    for( ICoercionProvider coercer: CoercionProviders.get() )
    {
      Object coercedValue = coercer.coerce( value, type );
      if( coercedValue != ICallHandler.UNHANDLED )
      {
        return coercedValue;
      }
    }
    return ICallHandler.UNHANDLED;
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
        for( Class superIface: iface.getInterfaces() )
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

    Map<Class, IProxyFactory<?,?>> proxyByClass = PROXY_CACHE.get( iface );
    if( proxyByClass == null )
    {
      PROXY_CACHE.put( iface, proxyByClass = new ConcurrentHashMap<>() );
    }
    IProxyFactory proxyFactory = proxyByClass.get( rootClass );
    if( proxyFactory == null )
    {
      proxyFactory = createProxy( iface, rootClass );
      proxyByClass.put( rootClass, proxyFactory );
    }
    try
    {
      // in Java 9+ in modular mode the proxy class belongs to the owner's module,
      // therefore we need to make it accessible from the manifold module before
      // calling newInstance()
      //noinspection unchecked
      return proxyFactory.proxy( root, iface );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private static IProxyFactory createProxy( Class iface, Class rootClass )
  {
    IProxyFactory proxyFactory = maybeSelfProxyClass( rootClass, iface );
    if( proxyFactory == null )
    {
      proxyFactory = new DynamicProxyFactory( iface, rootClass );
    }
    return proxyFactory;
  }

  private static IProxyFactory maybeSelfProxyClass( Class<?> rootClass, Class<?> iface )
  {
    // The self-proxy strategy avoids costs otherwise involved with generating and compiling the proxy at runtime via
    // ICallHandler

    Structural anno = iface.getAnnotation( Structural.class );
    if( anno != null )
    {
      Class factoryClass = anno.factoryClass();
      if( factoryClass != Void.class )
      {
        // If the proxy factory declared in @Structural handles the rootClass, create the proxy via the factory

        IProxyFactory proxyFactory = maybeMakeProxyFactory( rootClass, factoryClass, RuntimeMethods::constructProxyFactory );
        if( proxyFactory != null )
        {
          return proxyFactory;
        }
      }
    }

    // See if there is a registered IProxyFactory for the rootClass and iface, so create one that way,
    // otherwise return null

    return findRegisteredFactory( rootClass );
  }

  private static IProxyFactory findRegisteredFactory( Class<?> rootClass )
  {
    //noinspection ConstantConditions
    return _registeredProxyFactories.get().stream()
      .filter( e -> maybeMakeProxyFactory( rootClass, e.getClass(), c -> e ) != null )
      .findFirst().orElse( null );
  }

  private static IProxyFactory maybeMakeProxyFactory( Class<?> rootClass, Class factoryClass, Function<Class<?>, IProxyFactory> proxyFactoryMaker )
  {
    Type type = Arrays.stream( factoryClass.getGenericInterfaces() )
      .filter( e -> e.getTypeName().startsWith( IProxyFactory.class.getTypeName() ) )
      .findFirst().orElse( null );
    if( type instanceof ParameterizedType )
    {
      Type typeArg = ((ParameterizedType)type).getActualTypeArguments()[0];
      if( typeArg instanceof ParameterizedType )
      {
        typeArg = ((ParameterizedType)typeArg).getRawType();
      }
      if( ((Class<?>)typeArg).isAssignableFrom( rootClass ) )
      {
        return proxyFactoryMaker.apply( factoryClass );
      }
    }
    return null;
  }

  private static IProxyFactory constructProxyFactory( Class factoryClass )
  {
    try
    {
      // In Java 9+ in modular mode the proxy factory class belongs to the owner's module,
      // therefore we need to use the constructor and make it accessible from the manifold module
      // before calling newInstance() (as opposed to calling newInstance() from the class)
      Constructor constructor = factoryClass.getConstructors()[0];
      ReflectUtil.setAccessible( constructor );
      return (IProxyFactory)constructor.newInstance();
      //return (IProxyFactory)factoryClass.newInstance();
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public static Object coerceToBindingValue( Object arg )
  {
    if( arg instanceof IBindingType )
    {
      return ((IBindingType)arg).toBindingValue();
    }

    if( arg instanceof IBindingsBacked )
    {
      return arg;
    }

    if( arg instanceof List )
    {
      //noinspection unchecked
      return ((List)arg).stream()
        .map( e -> coerceToBindingValue( e ) )
        .collect( Collectors.toList() );
    }

    if( needsCoercion( arg ) )
    {
      for( ICoercionProvider coercer: CoercionProviders.get() )
      {
        Object coercedValue = coercer.toBindingValue( arg );
        if( coercedValue != ICallHandler.UNHANDLED )
        {
          return coercedValue;
        }
      }
    }

    return arg;
  }

  private static boolean needsCoercion( Object arg )
  {
    return arg != null &&
           !(arg instanceof Bindings) &&
           !isPrimitiveType( arg.getClass() );
  }

  private static boolean isPrimitiveType( Class<?> type )
  {
    return type == String.class ||
           type == Boolean.class ||
           type == Character.class ||
           type == Byte.class ||
           type == Short.class ||
           type == Integer.class ||
           type == Long.class ||
           type == Float.class ||
           type == Double.class;
  }
}
