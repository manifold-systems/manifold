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
import manifold.json.rt.api.IJsonList;
import manifold.json.rt.api.JsonList;

import java.util.List;

public class ListCoercer implements ICoercionProvider
{
  @Override
  public Object coerce( Object value, Class<?> type )
  {
    if( value instanceof List && !IListBacked.class.isAssignableFrom( type ) )
    {
      return new JsonList( (List)value, type );
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
