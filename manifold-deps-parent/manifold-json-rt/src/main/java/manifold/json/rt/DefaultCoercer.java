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
import manifold.ext.rt.api.IBindingType;
import manifold.json.rt.api.IJsonFormatTypeCoercer;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static manifold.ext.rt.api.ICallHandler.UNHANDLED;

/**
 */
public class DefaultCoercer implements IJsonFormatTypeCoercer
{
  private final LocklessLazyVar<Map<String, Class<?>>> _formatToType =
    LocklessLazyVar.make( () -> {
      // Standard temporal formats (JSON Schema 6)
      return new HashMap<String, Class<?>>() {{
      put( "date-time", LocalDateTime.class );
      put( "date", LocalDate.class );
      put( "time", LocalTime.class );
      // Non-standard temporal formats
      put( "full-date", LocalDateTime.class );
      put( "utc-millisec", Instant.class );
      // Non-standard number formats (see also BigNumberFormatResolver)
      put( "int64", Long.class );
    }}; } );

  @Override
  public Map<String, Class<?>> getFormats()
  {
    return _formatToType.get();
  }

  @Override
  public Object coerce( Object value, Type type )
  {
    //
    // From JSON value to Java value
    //

    Class rawType = type instanceof ParameterizedType ? (Class)((ParameterizedType)type).getRawType() : (Class)type;
    if( rawType.isEnum() )
    {
      Object v = coerceEnum( value, rawType );
      if( v != UNHANDLED )
      {
        return v;
      }
    }

    // "date-time"
    if( type == LocalDateTime.class )
    {
      return LocalDateTime.parse( String.valueOf( value ), DateTimeFormatter.ISO_DATE_TIME );
    }

    // "date"
    if( type == LocalDate.class )
    {
      return LocalDate.parse( String.valueOf( value ), DateTimeFormatter.ISO_DATE );
    }

    // "time"
    if( type == LocalTime.class )
    {
      return LocalTime.parse( String.valueOf( value ), DateTimeFormatter.ISO_TIME );
    }

    // "utc-millisec"
    if( type == Instant.class )
    {
      if( !(value instanceof Number) )
      {
        value = Long.valueOf( String.valueOf( value ) );
      }
      long millis = ((Number)value).longValue();
      long secs = millis / 1000;
      return Instant.ofEpochSecond( secs, (millis - secs * 1000) * 1000000 );
    }

    // "int64"
    if( type == Long.class || type == long.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).longValue();
      }
      if( value instanceof String )
      {
        return Long.valueOf( String.valueOf( value ) );
      }
    }

    //
    // From Java value to JSON value
    //

    // "date-time", "date", "time"
    if( value instanceof LocalDateTime ||
      value instanceof LocalDate ||
      value instanceof LocalTime )
    {
      if( type == String.class )
      {
        return value.toString();
      }
    }

    // "utc-millisec"
    if( value instanceof Instant )
    {
      if( Number.class.isAssignableFrom( rawType ) )
      {
        return RuntimeMethods.coerce( ((Instant)value).toEpochMilli(), rawType );
      }
      if( type == String.class )
      {
        return value.toString();
      }
    }

    // "int64"
    if( type == String.class && value instanceof Long )
    {
      return String.valueOf( value );
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

    if( value instanceof LocalDateTime ||
      value instanceof LocalDate ||
      value instanceof LocalTime)
    {
      return value.toString();
    }

    if( value instanceof Instant )
    {
      return ((Instant)value).toEpochMilli();
    }

    return UNHANDLED;
  }

  private Object coerceEnum( Object value, Class<?> type )
  {
    if( IBindingType.class.isAssignableFrom( type ) )
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
