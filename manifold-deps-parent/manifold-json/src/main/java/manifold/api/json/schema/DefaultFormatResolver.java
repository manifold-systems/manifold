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
import java.time.temporal.TemporalAccessor;
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
      formatToType.put( "date-time", new JsonFormatType( "date-time", LocalDateTime.class, new TypeAttributes( (Boolean)null, null ) ) );
      formatToType.put( "date", new JsonFormatType( "date", LocalDate.class, new TypeAttributes( (Boolean)null, null ) ) );
      formatToType.put( "time", new JsonFormatType( "time", LocalTime.class, new TypeAttributes( (Boolean)null, null ) ) );
      // Non-standard temporal formats
      formatToType.put( "full-date", new JsonFormatType( "full-date", LocalDateTime.class, new TypeAttributes( (Boolean)null, null ) ) );
      formatToType.put( "utc-millisec", new JsonFormatType( "utc-millisec", Instant.class, new TypeAttributes( (Boolean)null, null ) ) );
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
    if( type == LocalDateTime.class )
    {
      return LocalDateTime.parse( String.valueOf( value ), DateTimeFormatter.ISO_DATE_TIME );
    }
    if( type == LocalDate.class )
    {
      return LocalDate.parse( String.valueOf( value ), DateTimeFormatter.ISO_DATE );
    }
    if( type == LocalTime.class )
    {
      return LocalTime.parse( String.valueOf( value ), DateTimeFormatter.ISO_TIME );
    }
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

    //
    // From Java value to JSON value
    //
    if( value instanceof TemporalAccessor )
    {
      if( type == String.class )
      {
        return value.toString();
      }
      else if( value instanceof Instant &&
               Number.class.isAssignableFrom( type ) )
      {
        return RuntimeMethods.coerce( ((Instant)value).toEpochMilli(), type );
      }
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
