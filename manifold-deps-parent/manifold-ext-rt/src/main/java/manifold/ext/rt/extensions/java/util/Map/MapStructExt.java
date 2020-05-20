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

import java.util.Arrays;
import java.util.Map;
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
  public static <K, V> Object call( @This Map<K, V> thiz, Class<?> iface, String name, String actualName,
                                    Class<?> returnType, Class<?>[] paramTypes, Object[] args )
  {
    assert paramTypes.length == args.length;

    Object value = ICallHandler.UNHANDLED;
    if( returnType != void.class && paramTypes.length == 0 )
    {
      // call getter
      value = getValue( thiz, name, actualName );
    }
    if( value == ICallHandler.UNHANDLED )
    {
      if( returnType == void.class )
      {
        // call setter
        value = setValue( thiz, name, actualName, paramTypes, args );
      }
      if( value == ICallHandler.UNHANDLED )
      {
        // invoke single method implementor e.g., a lambda
        value = invoke( thiz, name, args );
      }
    }
    if( value == ICallHandler.UNHANDLED )
    {
      value = RuntimeMethods.invokeUnhandled( thiz, iface, name, returnType, paramTypes, args );
    }
    if( value == ICallHandler.UNHANDLED )
    {
      throw new RuntimeException( "Missing method: " + name + "(" + Arrays.toString( paramTypes ) + ")" );
    }
    return value;
  }

  private static Object getValue( Map thiz, String name, String actualName )
  {
    Object value;
    value = getValue( thiz, name, actualName, "get" );
    if( value == ICallHandler.UNHANDLED )
    {
      value = getValue( thiz, name, actualName, "is" );
    }
    return value;
  }

  private static Object getValue( Map thiz, String name, String actualName, String prefix )
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
          return thiz.get( actualName );
        }

        String key = name.substring( getLen );
        if( thiz.containsKey( key ) )
        {
          return thiz.get( key );
        }
        key = Character.toLowerCase( c ) + name.substring( 1 );
        if( thiz.containsKey( key ) )
        {
          return thiz.get( key );
        }
        return null;
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Object setValue( Map thiz, String name, String actualName, Class<?>[] paramTypes, Object[] args )
  {
    int setLen = "set".length();
    if( paramTypes.length == 1 && name.length() > setLen && name.startsWith( "set" ) )
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
          if( thiz.containsKey( upperKey ) )
          {
            key = upperKey;
          }
          else
          {
            String lowerKey = Character.toLowerCase( c ) + name.substring( 1 );
            if( thiz.containsKey( lowerKey ) )
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
        if( thiz instanceof Bindings )
        {
          arg = RuntimeMethods.coerceToBindingValue( arg );
        }
        //noinspection unchecked
        thiz.put( key, arg );
        return null;
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Object invoke( Map thiz, String name, Object[] args )
  {
    Object value = thiz.get( name );
    if( value == null )
    {
      return ICallHandler.UNHANDLED;
    }
    //noinspection ConstantConditions
    return ReflectUtil.lambdaMethod( value.getClass() ).invoke( value, args );
  }
}
