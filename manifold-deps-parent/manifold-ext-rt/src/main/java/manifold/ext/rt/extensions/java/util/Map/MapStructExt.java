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

package manifold.ext.rt.extensions.java.util.Map;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.rt.api.ActualName;
import manifold.rt.api.Bindings;
import manifold.ext.rt.RuntimeMethods;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.ICallHandler;
import manifold.ext.rt.api.This;
import manifold.util.ReflectUtil;

/**
 * Interface extension for java.util.Map to add ICallHandler support.
 */
@Extension
public abstract class MapStructExt implements ICallHandler
{
  public static <K, V> Object call( @This Map<K, V> bindings, Object proxy, Class<?> iface, String name, String actualName,
                                    Class<?> returnType, Class<?>[] paramTypes, Object[] args )
  {
    assert paramTypes.length == args.length;

    return invoke( bindings, proxy, name, actualName, returnType, returnType, paramTypes, args );
  }

  public static Object invoke( Map bindings, Object proxy, Method method, Object[] args )
  {
    assert method.getParameterCount() == (args == null ? 0 : args.length);

    String methodName = method.getName();
    Object result;
    if( method.isDefault() )
    {
      result = ReflectUtil.invokeDefault( proxy, method, args );
    }
    else
    {
      String actualName = null;
      if( methodName.startsWith( "get" ) || methodName.startsWith( "is" ) || methodName.startsWith( "set" ) ||
        methodName.startsWith( "with" ) )
      {
        actualName = getActualName( method );
      }
      result = invoke( bindings, proxy, method.getName(), actualName, method.getReturnType(), method.getGenericReturnType(), method.getParameterTypes(), args );
    }
    return result;
  }

  private static Object invoke( Map bindings, Object proxy, String methodName, String actualName, Class<?> returnType, Type genReturnType, Class[] paramTypes, Object[] args )
  {
    Object result = ICallHandler.UNHANDLED;

    if( proxy instanceof IBindingsBacked && methodName.equals( "getBindings" ) )
    {
      result = bindings;
    }
    else if( returnType != void.class && paramTypes.length == 0 )
    {
      // call getter
      result = getValue( bindings, methodName, actualName, genReturnType );
    }
    if( result == ICallHandler.UNHANDLED )
    {
      if( returnType == void.class || returnType.isAssignableFrom( proxy.getClass() ) )
      {
        // call setter
        result = setValue( bindings, methodName, actualName, paramTypes, args );
        if( returnType != void.class )
        {
          result = bindings; //proxy;
        }
      }
      if( result == ICallHandler.UNHANDLED )
      {
        // invoke single method implementor e.g., a lambda
        result = invoke( bindings, methodName, args );
      }
    }
    if( result == ICallHandler.UNHANDLED )
    {
      switch( methodName )
      {
        case "hashCode":
          result = _hashCode( bindings );
          break;
        case "equals":
          result = _equals( bindings, args[0] );
          break;
        case "toString":
          result = _toString( bindings );
          break;
      }
    }
    if( result == ICallHandler.UNHANDLED )
    {
      throw new RuntimeException( "Missing method: " + methodName + "(" + Arrays.toString( paramTypes ) + ")" );
    }
    return result;
  }

  private static Object getValue( Map bindings, String name, String actualName, Type returnType )
  {
    Object value;
    value = getValue( bindings, name, actualName, "get" );
    if( value == ICallHandler.UNHANDLED )
    {
      value = getValue( bindings, name, actualName, "is" );
    }
    if( value != ICallHandler.UNHANDLED )
    {
      value = RuntimeMethods.coerceFromBindingsValue( value, returnType );
    }
    return value;
  }

  private static Object getValue( Map bindings, String name, String actualName, String prefix )
  {
    int getLen = prefix.length();
    if( name.length() > getLen && name.startsWith( prefix ) )
    {
      char c = name.charAt( getLen );
      if( c == '_' && name.length() > getLen + 1 )
      {
        getLen++;
        c = Character.toUpperCase( name.charAt( getLen ) );
      }
      if( Character.isUpperCase( c ) )
      {
        if( actualName != null )
        {
          return bindings.get( actualName );
        }

        String key = name.substring( getLen );
        if( bindings.containsKey( key ) )
        {
          return bindings.get( key );
        }
        key = Character.toLowerCase( c ) + name.substring( 1 );
        if( bindings.containsKey( key ) )
        {
          return bindings.get( key );
        }
        return null;
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Object setValue( Map bindings, String name, String actualName, Class<?>[] paramTypes, Object[] args )
  {
    Object result = setValue( bindings, "set", name, actualName, paramTypes, args );
    if( result == ICallHandler.UNHANDLED )
    {
      result = setValue( bindings, "with", name, actualName, paramTypes, args );
    }
    return result;
  }
  private static Object setValue( Map bindings, String prefix, String name, String actualName, Class<?>[] paramTypes, Object[] args )
  {
    int setLen = prefix.length();
    if( paramTypes.length == 1 && name.length() > setLen && name.startsWith( prefix ) )
    {
      char c = name.charAt( setLen );
      if( c == '_' && name.length() > setLen + 1 )
      {
        setLen++;
        c = Character.toUpperCase( name.charAt( setLen ) );
      }
      String key;
      if( Character.isUpperCase( c ) )
      {
        if( actualName != null )
        {
          key = actualName;
        }
        else
        {
          String upperKey = name.substring( setLen );
          if( bindings.containsKey( upperKey ) )
          {
            key = upperKey;
          }
          else
          {
            String lowerKey = Character.toLowerCase( c ) + name.substring( 1 );
            if( bindings.containsKey( lowerKey ) )
            {
              key = lowerKey;
            }
            else
            {
              key = upperKey;
            }
          }
        }
        Object arg = args[0];
        if( bindings instanceof Bindings )
        {
          arg = RuntimeMethods.coerceToBindingValue( arg );
        }
        //noinspection unchecked
        bindings.put( key, arg );
        return null;
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Object invoke( Map bindings, String name, Object[] args )
  {
    Object value = bindings.get( name );
    if( value == null )
    {
      return ICallHandler.UNHANDLED;
    }
    //noinspection ConstantConditions
    return ReflectUtil.lambdaMethod( value.getClass() ).invoke( value, args );
  }

  private static String getActualName( Method method )
  {
    ActualName actualNameAnno = method.getAnnotation( ActualName.class );
    return actualNameAnno == null ? null : actualNameAnno.value();
  }

  private static String _toString( Map bindings )
  {
    return bindings.toString();
  }

  private static int _hashCode( Map bindings )
  {
    return bindings.hashCode();
  }

  private static boolean _equals( Map bindings, Object obj )
  {
    return obj instanceof Bindings
      ? obj.equals( bindings )
      : obj instanceof IBindingsBacked && bindings.equals( ((IBindingsBacked)obj).getBindings() );
  }
}
