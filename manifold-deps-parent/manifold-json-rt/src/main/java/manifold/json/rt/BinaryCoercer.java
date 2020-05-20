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


import manifold.json.rt.api.Base64Encoding;
import manifold.json.rt.api.IJsonFormatTypeCoercer;
import manifold.json.rt.api.OctetEncoding;

import java.util.HashMap;
import java.util.Map;

import static manifold.ext.rt.api.ICallHandler.UNHANDLED;

/**
 * Implement {@code "binary"} and {@code "byte"} formats.
 * Maps them to {@link OctetEncoding} and {@link Base64Encoding} respectively.
 * <p>
 * These formats are defined in <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc0/versions/3.0.md#schemaObject">OpenAPI Specification</a>
 */
public class BinaryCoercer implements IJsonFormatTypeCoercer
{
  private static final Map<String,Class<?>> ALL = new HashMap<String, Class<?>>() {{
    put( "binary", OctetEncoding.class );
    put( "byte", Base64Encoding.class );
  }};

  @Override
  public Map<String, Class<?>> getFormats()
  {
    return ALL;
  }

  @Override
  public Object coerce( Object value, Class<?> type )
  {
    //
    // From JSON value to Java value
    //
    if( type == OctetEncoding.class && value instanceof String )
    {
      return OctetEncoding.encoded( (String)value );
    }
    if( type == Base64Encoding.class && value instanceof String )
    {
      return Base64Encoding.encoded( (String)value );
    }

    //
    // From Java value to JSON value
    //
    if( value instanceof OctetEncoding ||
        value instanceof Base64Encoding )
    {
      if( type == String.class )
      {
        return value.toString();
      }
    }

    return UNHANDLED;
  }

  @Override
  public Object toBindingValue( Object value )
  {
    if( value instanceof OctetEncoding ||
        value instanceof Base64Encoding )
    {
      return value.toString();
    }

    return UNHANDLED;
  }
}
