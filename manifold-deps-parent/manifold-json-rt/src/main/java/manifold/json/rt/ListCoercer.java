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
import manifold.ext.rt.api.ICallHandler;
import manifold.ext.rt.api.ICoercionProvider;
import manifold.ext.rt.api.IListBacked;
import manifold.json.rt.api.JsonList;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class ListCoercer implements ICoercionProvider
{
  @Override
  public Object coerce( Object value, Type toType )
  {
    Class<?> toClass = toType instanceof ParameterizedType
      ? (Class)((ParameterizedType)toType).getRawType()
      : (Class)toType;
    if( !toClass.isInterface() )
    {
      return ICallHandler.UNHANDLED;
    }

    while( toType instanceof ParameterizedType && value instanceof List &&
      List.class.isAssignableFrom( (Class)((ParameterizedType)toType).getRawType() ) )
    {
      toType = ((ParameterizedType)toType).getActualTypeArguments()[0];
    }
    if( toType instanceof ParameterizedType )
    {
      toType = ((ParameterizedType)toType).getRawType();
    }
    Class rawToType = (Class)toType;
    if( value instanceof List )
    {
      // handle case like Person.Hobby where Hobby extends IListBacked<HobbyItem>
      if( IListBacked.class.isAssignableFrom( rawToType ) )
      {
        return RuntimeMethods.constructProxy( value, rawToType );
      }

      // Handle case like Foo where Foo is the component type to transform a simple List to a JsonList<Foo>
      return new JsonList( (List)value, rawToType );
    }

    return ICallHandler.UNHANDLED;
  }

  @Override
  public Object toBindingValue( Object value )
  {
    if( value instanceof JsonList )
    {
      return ((JsonList) value).getList();
    }
    return ICallHandler.UNHANDLED;
  }
}
