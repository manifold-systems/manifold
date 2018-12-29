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


import java.math.BigDecimal;
import java.math.BigInteger;


import static manifold.ext.api.ICallHandler.UNHANDLED;

/**
 * Implement {@code "big-integer"} and {@code "big-decimal"} formats. Maps them to corresponding Java
 * classes {@link BigInteger} and {@link BigDecimal}. Note these are non-standard JSON Schema
 * formats, but they fill a void where an API must use "big" numbers instead of {@code "integer"}
 * or {@code "number"}.
 */
public class BigNumberFormatResolver implements IJsonFormatTypeResolver
{
  private static final JsonFormatType BIG_INTEGER = new JsonFormatType( "big-integer", BigInteger.class );
  private static final JsonFormatType BIG_DECIMAL = new JsonFormatType( "big-decimal", BigDecimal.class );

  @Override
  public JsonFormatType resolveType( String format )
  {
    if( BIG_INTEGER.getFormat().equals( format ) )
    {
      return BIG_INTEGER;
    }
    if( BIG_DECIMAL.getFormat().equals( format ) )
    {
      return BIG_DECIMAL;
    }
    return null;
  }

  @Override
  public Object coerce( Object value, Class<?> type )
  {
    //
    // From JSON value to Java value
    //
    if( type == BigInteger.class )
    {
      return "0".equals( value ) ? BigInteger.ZERO : new BigInteger( String.valueOf( value ) );
    }
    if( type == BigDecimal.class )
    {
      return "0".equals( value ) ? BigDecimal.ZERO : new BigDecimal( String.valueOf( value ) );
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
