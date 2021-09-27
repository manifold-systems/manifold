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
  public Object coerce( Object value, Type type )
  {
    while( type instanceof ParameterizedType && value instanceof List &&
      List.class.isAssignableFrom( (Class)((ParameterizedType)type).getRawType() ) )
    {
      type = ((ParameterizedType)type).getActualTypeArguments()[0];
    }
    if( type instanceof ParameterizedType )
    {
      type = ((ParameterizedType)type).getRawType();
    }
    Class rawType = (Class)type;
    if( value instanceof List && !IListBacked.class.isAssignableFrom( rawType ) )
    {
      return new JsonList( (List)value, rawType );
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
