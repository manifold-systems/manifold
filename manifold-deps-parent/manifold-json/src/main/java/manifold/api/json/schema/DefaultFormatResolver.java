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

package manifold.api.json.schema;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import manifold.ext.RuntimeMethods;
import manifold.util.concurrent.LocklessLazyVar;


import static manifold.ext.api.ICallHandler.UNHANDLED;

/**
 * Handles standard JSON Schema formats such as {@code "date-time", "date", "time"}, maps them to corresponding Java
 * classes {@link LocalDateTime}, {@link LocalDate}, and {@link LocalTime}. Also handles some non-standard
 * but commonly used formats such as {@code "full-date" and "utc-millisec"}.
 */
public class DefaultFormatResolver implements IJsonFormatTypeResolver
{
  private final LocklessLazyVar<Map<String, JsonFormatType>> _formatToType =
    LocklessLazyVar.make( () -> {
      // Standard temporal formats (JSON Schema 6)
      Map<String, JsonFormatType> formatToType = new HashMap<>();
      formatToType.put( "date-time", new JsonFormatType( "date-time", LocalDateTime.class ) );
      formatToType.put( "date", new JsonFormatType( "date", LocalDate.class ) );
      formatToType.put( "time", new JsonFormatType( "time", LocalTime.class ) );
      // Non-standard temporal formats
      formatToType.put( "full-date", new JsonFormatType( "full-date", LocalDateTime.class ) );
      formatToType.put( "utc-millisec", new JsonFormatType( "utc-millisec", Instant.class ) );
      // Non-standard number formats (see also BigNumberFormatResolver)
      formatToType.put( "int64", new JsonFormatType( "int64", Long.class ) );
      return formatToType;
    } );

  @Override
  public JsonFormatType resolveType( String format )
  {
    //noinspection ConstantConditions
    return _formatToType.get().get( format );
  }

  @Override
  public Object coerce( Object value, Class<?> type )
  {
    //
    // From JSON value to Java value
    //

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
      if( Number.class.isAssignableFrom( type ) )
      {
        return RuntimeMethods.coerce( ((Instant)value).toEpochMilli(), type );
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
}
