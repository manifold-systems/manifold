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

package manifold.api.json;

/**
 */
public enum JsonSimpleType implements IJsonType
{
  String,
  Boolean,
  Character,
  Byte,
  Short,
  Integer,
  Long,
  Float,
  Double,
  BigDecimal,
  BigInteger;

  @Override
  public String getName()
  {
    return super.name();
  }

  @Override
  public String getIdentifier()
  {
    return getName();
  }

  @Override
  public IJsonParentType getParent()
  {
    return null;
  }

  public static JsonSimpleType get( Object jsonObj )
  {
    if( jsonObj == null )
    {
      return null;
    }

    return valueOf( JsonSimpleType.class, jsonObj.getClass().getSimpleName() );
  }

  JsonSimpleType merge( JsonSimpleType other )
  {
    if( this == JsonSimpleType.String ||
        other == JsonSimpleType.String )
    {
      // String is compatible with all simple types
      return JsonSimpleType.String;
    }

    switch( this )
    {
      case Boolean:
        // Boolean is only compatible with String
        break;

      case Character:
        switch( other )
        {
          case Byte:
            return this;
          case Short:
          case Integer:
          case Long:
          case Float:
          case Double:
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case Byte:
        switch( other )
        {
          case Character:
          case Short:
          case Integer:
          case Long:
          case Float:
          case Double:
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case Short:
        switch( other )
        {
          case Character:
          case Byte:
            return this;
          case Integer:
          case Long:
          case Float:
          case Double:
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case Integer:
        switch( other )
        {
          case Character:
          case Byte:
          case Short:
            return this;
          case Long:
          case Float:
          case Double:
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case Long:
        switch( other )
        {
          case Character:
          case Byte:
          case Short:
          case Integer:
            return this;
          case Float:
          case Double:
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case Float:
        switch( other )
        {
          case Character:
          case Byte:
          case Short:
          case Integer:
          case Long:
            return this;
          case Double:
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case Double:
        switch( other )
        {
          case Character:
          case Byte:
          case Short:
          case Integer:
          case Long:
          case Float:
            return this;
          case BigDecimal:
          case BigInteger:
            return other;
        }
        break;

      case BigDecimal:
        switch( other )
        {
          case Character:
          case Byte:
          case Short:
          case Integer:
          case Long:
          case Float:
          case Double:
          case BigInteger:
            return this;
        }
        break;

      case BigInteger:
        switch( other )
        {
          case Character:
          case Byte:
          case Short:
          case Integer:
          case Long:
            return this;
          case Float:
          case Double:
          case BigDecimal:
            return BigDecimal;
        }
        break;
    }
    return null;
  }
}
