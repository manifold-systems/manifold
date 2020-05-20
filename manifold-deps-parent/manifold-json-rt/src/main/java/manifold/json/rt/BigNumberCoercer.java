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

import manifold.json.rt.api.IJsonFormatTypeCoercer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static manifold.ext.rt.api.ICallHandler.UNHANDLED;

/**
 * Implement {@code "big-integer"} and {@code "big-decimal"} formats. Maps them to corresponding Java
 * classes {@link BigInteger} and {@link BigDecimal}. Note these are non-standard JSON Schema
 * formats, but they fill a void where an API must use "big" numbers instead of {@code "integer"}
 * or {@code "number"}.
 */
public class BigNumberCoercer implements IJsonFormatTypeCoercer
{
  private static final Map<String,Class<?>> ALL = new HashMap<String, Class<?>>() {{
    put( "big-integer", BigInteger.class );
    put( "big-decimal", BigDecimal.class );
  }};

  @Override
  public Map<String,Class<?>> getFormats()
  {
    return ALL;
  }

  @Override
  public Object coerce( Object value, Class<?> type )
  {
    //
    // From JSON value to Java value
    //
    if( type == BigInteger.class && value instanceof String )
    {
      return "0" .equals( value ) ? BigInteger.ZERO : new BigInteger( (String)value );
    }
    if( type == BigDecimal.class && value instanceof String )
    {
      return "0" .equals( value ) ? BigDecimal.ZERO : new BigDecimal( (String)value );
    }

    //
    // From Java value to JSON value
    //
    if( value instanceof BigInteger ||
        value instanceof BigDecimal )
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
    if( value instanceof BigInteger ||
        value instanceof BigDecimal )
    {
      return value.toString();
    }

    return UNHANDLED;
  }
}
