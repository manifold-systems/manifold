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

package manifold.ext.rt;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import manifold.ext.rt.extensions.java.util.Map.MapStructExt;
import manifold.rt.api.Bindings;

import manifold.rt.api.util.ManClassUtil;
import manifold.ext.rt.api.*;
import manifold.rt.api.util.ServiceUtil;
import manifold.util.CoerceUtil;
import manifold.util.ReflectUtil;
import manifold.util.ReflectUtil.FakeProxy;
import manifold.util.concurrent.LocklessLazyVar;

public class RuntimeMethods
{
  private static Map<Class, Map<Class, IProxyFactory<?,?>>> PROXY_CACHE = new ConcurrentHashMap<>();
  private static final LocklessLazyVar<Set<IProxyFactory>> _registeredProxyFactories =
    LocklessLazyVar.make( () -> {
      Set<IProxyFactory> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, IProxyFactory.class, RuntimeMethods.class.getClassLoader() );
      return registered;
    } );
  private static final LocklessLazyVar<Set<IProxyFactory_gen>> _registeredProxyFactories_gen =
    LocklessLazyVar.make( () -> {
      Set<IProxyFactory_gen> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, IProxyFactory_gen.class, RuntimeMethods.class.getClassLoader() );
      return registered;
    } );
  private static final LocklessLazyVar<IDynamicProxyFactory> _dynamicProxyFactory =
    LocklessLazyVar.make( () -> {
      Set<IDynamicProxyFactory> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, IDynamicProxyFactory.class, RuntimeMethods.class.getClassLoader() );
      return registered.isEmpty() ? null : registered.iterator().next();
    } );

  @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
  public static Object constructProxy( Object root, Class iface )
  {
    // return findCachedProxy( root, iface ); // this is only beneficial when structural invocation happens in a loop, otherwise too costly
    return createNewProxy( root, iface );
  }

  public static Object coerceFromBindingsValue( Object value, Type t )
  {
//## would like to do this to limit proxies to just structural calls, however since we support default interface methods
//## we have a situation where a more specific interface "implements" using a default method a less specific interface,
//## however if a bindings/map is cast to the less specific one, there the default impl isn't there... boom. Therefore,
//## we have to keep the proxies alive, thereby allowing such casts to work.
//    if( value instanceof Bindings )
//    {
//      return value;
//    }

    return coerce( value, t );
  }
  
  /**
   * Coerce a value e.g., from a JSON bindings, to a more specific a Java value, using {@link ICoercionProvider}
   * where applicable. Note, for {@code List} the {@code type} corresponds with the deepest component type of the list,
   * see {@code ListCoercer}.
   */
  public static Object coerce( Object value, Type t )
  {
    Class<?> type = t instanceof ParameterizedType ? (Class<?>)((ParameterizedType)t).getRawType() : (Class)t;
    if( value == null )
    {
      if( type.isPrimitive() )
      {
        return defaultPrimitiveValue( type );
      }
      return null;
    }

    if( value instanceof List )
    {
      Object result = callCoercionProviders( value, t );
      if( result != ICallHandler.UNHANDLED )
      {
        return result;
      }
      return value;
    }

    if( type.isPrimitive() )
    {
      type = ManClassUtil.box( type );
    }

    Class<?> valueClass = value.getClass();
    if( valueClass == type || type.isAssignableFrom( valueClass ) )
    {
      return value;
    }

    Object result = callCoercionProviders( value, t );
    if( result != ICallHandler.UNHANDLED )
    {
      return result;
    }

    if( value instanceof String && ((String)value).isEmpty() && type != String.class )
    {
      // empty string is null e.g., CSV empty values are empty strings
      return null;
    }

    Object boxedValue = CoerceUtil.coerceBoxed( value, type );
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

  private static Object callCoercionProviders( Object value, Type type )
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
      IDynamicProxyFactory dynamicProxyFactory = _dynamicProxyFactory.get();
      if( dynamicProxyFactory == null || rootClass.isAnonymousClass() )
      {
        // No Manifold runtime operating (manifold was used exclusively at compile-time), so we use a proxy that calls dynamically at runtime
        return makeDynamicProxyNoManifoldRuntimeHost( rootClass, iface );
      }
      proxyFactory = dynamicProxyFactory.makeProxyFactory( iface, rootClass );
    }
    return proxyFactory;
  }

  private static IProxyFactory makeDynamicProxyNoManifoldRuntimeHost( Class rootClass, Class intface )
  {
    if( Map.class.isAssignableFrom( rootClass ) )
    {
      return (target, iface) -> manifold.ext.rt.proxy.Proxy.newProxyInstance( intface.getClassLoader(), new Class[]{iface},
        (proxy, method, args) -> MapStructExt.invoke( (Map)target, proxy, method, args) );
    }
    if( List.class.isAssignableFrom( rootClass ) )
    {
      return (target, iface) -> manifold.ext.rt.proxy.Proxy.newProxyInstance( intface.getClassLoader(), new Class[]{iface},
        (proxy, method, args) -> proxy instanceof IListBacked
          ? ListProxy.invoke( (List)target, proxy, method, args)
          : ReflectUtil.structuralCallByProxy( method, proxy, target, args ) );
    }
    return (target, iface) -> manifold.ext.rt.proxy.Proxy.newProxyInstance( intface.getClassLoader(), new Class[]{iface},
      (proxy, method, args) -> ReflectUtil.structuralCallByProxy( method, proxy, target, args ) );
  }

  public static IProxyFactory maybeSelfProxyClass( Class<?> rootClass, Class<?> iface )
  {
    // The self-proxy strategy avoids costs otherwise involved with generating a proxy dynamically,
    // and since it's compiled statically with manifold, the proxy source can use manifold features
    // such as extension methods, operator overloading, etc.

    Structural anno = iface.getAnnotation( Structural.class );
    if( anno != null )
    {
      Class factoryClass = anno.factoryClass();
      if( factoryClass != Void.class )
      {
        // If the proxy factory declared in @Structural handles the rootClass, create the proxy via the factory

        IProxyFactory proxyFactory = maybeMakeProxyFactory( rootClass, iface, factoryClass, RuntimeMethods::constructProxyFactory );
        if( proxyFactory != null )
        {
          return proxyFactory;
        }
      }
    }

    // See if there is a registered IProxyFactory for the rootClass and iface, so create one that way,
    // otherwise return null

    return findRegisteredFactory( rootClass, iface );
  }

  private static IProxyFactory findRegisteredFactory( Class<?> rootClass, Class<?> iface )
  {
    //noinspection ConstantConditions
    return _registeredProxyFactories.get().stream()
      .filter( e -> !(e instanceof IDynamicProxyFactory) )
      .filter( e -> maybeMakeProxyFactory( rootClass, iface, e.getClass(), c -> e ) != null )
      .findFirst().orElse(
        _registeredProxyFactories_gen.get().stream()
          .filter( e -> maybeMakeProxyFactory( rootClass, iface, e.getClass(), c -> e ) != null )
          .findFirst().orElse( null ) );
  }

  private static IProxyFactory maybeMakeProxyFactory( Class<?> rootClass, Class<?> ifaceClass, Class factoryClass,
                                                      Function<Class<?>, IProxyFactory> proxyFactoryMaker )
  {
    Type type = Arrays.stream( factoryClass.getGenericInterfaces() )
      .filter( e -> e.getTypeName().startsWith( IProxyFactory.class.getTypeName() ) )
      .findFirst().orElse( null );
    if( type instanceof ParameterizedType )
    {
      Type typeArg1 = ((ParameterizedType)type).getActualTypeArguments()[0];
      if( typeArg1 instanceof ParameterizedType )
      {
        typeArg1 = ((ParameterizedType)typeArg1).getRawType();
      }
      if( !((Class<?>)typeArg1).isAssignableFrom( rootClass ) )
      {
        return null;
      }

      Type typeArg2 = ((ParameterizedType)type).getActualTypeArguments()[1];
      if( typeArg2 instanceof ParameterizedType )
      {
        typeArg2 = ((ParameterizedType)typeArg2).getRawType();
      }
      if( ((Class<?>)typeArg2).isAssignableFrom( ifaceClass ) )
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
      return ((IBindingsBacked)arg).getBindings();
    }

    if( arg instanceof IListBacked )
    {
      return ((IListBacked) arg).getList();
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

  @SuppressWarnings( "unused" )
  public static Object unFakeProxy( Object proxy )
  {
    if( proxy == null || !Proxy.isProxyClass( proxy.getClass() ) )
    {
      return proxy;
    }
    InvocationHandler invocationHandler = Proxy.getInvocationHandler( proxy );
    if( invocationHandler instanceof FakeProxy )
    {
      return ((FakeProxy)invocationHandler).getTarget();
    }
    return proxy;
  }

  @SuppressWarnings( "unused" )
  public static <T> Iterable<T> makeIterable( Iterator<T> iter )
  {
    return () -> iter;
  }
}
