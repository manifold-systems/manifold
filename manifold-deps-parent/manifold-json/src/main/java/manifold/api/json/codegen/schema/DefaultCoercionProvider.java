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

package manifold.api.json.codegen.schema;

import manifold.ext.RuntimeMethods;
import manifold.ext.api.IBindingType;
import manifold.ext.api.ICoercionProvider;
import manifold.util.ReflectUtil;


import static manifold.ext.api.ICallHandler.UNHANDLED;

/**
 * Handles coercions for JSON {@code enum} types and delegates to {@link IJsonFormatTypeResolver} service providers
 * for {@link JsonFormatType} coercions.
 */
public class DefaultCoercionProvider implements ICoercionProvider
{
  @Override
  public Object coerce( Object value, Class<?> type )
  {
    // Handle enum type
    Object enumConst = coerceEnum( value, type );
    if( enumConst != UNHANDLED )
    {
      return enumConst;
    }

    // Handle format types
    for( IJsonFormatTypeResolver resolver: FormatTypeResolvers.get() )
    {
      Object coercedValue = resolver.coerce( value, type );
      if( coercedValue != UNHANDLED )
      {
        return coercedValue;
      }
    }
    return UNHANDLED;
  }

  @Override
  public Object toBindingValue( Object value )
  {
    // Enum type & Union type
    if( value instanceof IBindingType )
    {
      return ((IBindingType)value).toBindingValue();
    }

    // Format types
    for( IJsonFormatTypeResolver resolver: FormatTypeResolvers.get() )
    {
      Object coercedValue = resolver.toBindingValue( value );
      if( coercedValue != UNHANDLED )
      {
        return coercedValue;
      }
    }
    return UNHANDLED;
  }

  private Object coerceEnum( Object value, Class<?> type )
  {
    if( type.isEnum() && IBindingType.class.isAssignableFrom( type ) )
    {
      if( IBindingType.class.isAssignableFrom( type ) )
      {
        //noinspection ConstantConditions
        IBindingType[] values = (IBindingType[])ReflectUtil.method( type, "values" ).invokeStatic();
        for( IBindingType enumConst: values )
        {
          Object jsonValue = enumConst.toBindingValue();
          Object coercedValue = RuntimeMethods.coerce( value, jsonValue.getClass() );
          if( jsonValue.equals( coercedValue ) )
          {
            return enumConst;
          }
        }
      }
    }
    return UNHANDLED;
  }
}
