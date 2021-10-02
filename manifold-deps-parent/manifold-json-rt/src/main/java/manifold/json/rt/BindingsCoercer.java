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

package manifold.json.rt;

import manifold.ext.rt.RuntimeMethods;
import manifold.ext.rt.api.*;
import manifold.rt.api.Bindings;
import manifold.util.ReflectUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Coerce {@link Bindings} and {@link List} to {@link IBindingsBacked} and {@link IListBacked}.
 * <p/>
 * Note, this coercer primarily serves the JSON and GraphQL APIs where structural interfaces overlay bindings. This
 * coercer is used to produce direct instances of the interfaces so that other JVM languages can use JSON and GraphQL
 * type manifolds without having to support structural typing.
 */
public class BindingsCoercer implements ICoercionProvider
{
  @Override
  public Object coerce( Object o, Type ifaceToProxyType )
  {
    Class<?> ifaceToProxy = ifaceToProxyType instanceof ParameterizedType
      ? (Class)((ParameterizedType)ifaceToProxyType).getRawType()
      : (Class)ifaceToProxyType;
    if( !ifaceToProxy.isInterface() )
    {
      return ICallHandler.UNHANDLED;
    }

    Structural annotation = ifaceToProxy.getAnnotation( Structural.class );
    if( annotation != null )
    {
      if( o instanceof Bindings && IBindingsBacked.class.isAssignableFrom( ifaceToProxy ) )
      {
        if( annotation.factoryClass() != Void.class )
        {
          IProxyFactory factory = (IProxyFactory)ReflectUtil.constructor( ifaceToProxy.getName() + "$ProxyFactory" ).newInstance();
          //noinspection unchecked
          return factory.proxy( o, ifaceToProxy );
        }
        else
        {
          return RuntimeMethods.constructProxy( o, ifaceToProxy );
        }
      }
    }
    return ICallHandler.UNHANDLED;
  }

  @Override
  public Object toBindingValue( Object value )
  {
    if( value instanceof IBindingsBacked )
    {
      return ((IBindingsBacked)value).getBindings();
    }
    if( value instanceof IListBacked )
    {
      return ((IListBacked)value).getList();
    }
    return ICallHandler.UNHANDLED;
  }
}
