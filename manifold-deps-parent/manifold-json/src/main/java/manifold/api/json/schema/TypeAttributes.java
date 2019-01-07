/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

import java.util.Objects;
import manifold.api.json.IJsonType;

public final class TypeAttributes
{
  private Boolean _nullable;
  private Object _defaultValue;

  public TypeAttributes( Boolean nullable, Object defaultValue )
  {
    _nullable = nullable;
    _defaultValue = defaultValue;
  }

  public TypeAttributes( IJsonType t1, IJsonType t2 )
  {
    Boolean nullable = t1.getTypeAttributes().getNullable();
    if( t2.getTypeAttributes().getNullable() != null )
    {
      if( nullable != null )
      {
        nullable &= t2.getTypeAttributes().getNullable();
      }
      else
      {
        nullable = t2.getTypeAttributes().getNullable();
      }
    }
    _nullable = nullable;
    _defaultValue = t1.getTypeAttributes().getDefaultValue() == null
                    ? t2.getTypeAttributes().getDefaultValue()
                    : t1.getTypeAttributes().getDefaultValue();
  }

  public static TypeAttributes merge( TypeAttributes t1, TypeAttributes t2 )
  {
    Boolean nullableValue = t1.getNullable() == null ? t2.getNullable() : t1.getNullable();
    Object defaultValue = t1.getDefaultValue() == null ? t2.getDefaultValue() : t1.getDefaultValue();
    return new TypeAttributes( nullableValue, defaultValue );
  }

  public TypeAttributes copy()
  {
    return new TypeAttributes( _nullable, _defaultValue );
  }

  public Boolean getNullable()
  {
    return _nullable;
  }
  public void setNullable( Boolean nullable )
  {
    _nullable = nullable;
  }

  public Object getDefaultValue()
  {
    return _defaultValue;
  }
  public void setDefaultValue( Object value )
  {
    _defaultValue = value;
  }

  public static Boolean or( Boolean b1, Boolean b2 )
  {
    Boolean nullableValue = b1;
    if( nullableValue == null )
    {
      nullableValue = b2;
    }
    else if( b2 != null )
    {
      nullableValue |= b2;
    }
    return nullableValue;
  }

  public static Boolean and( Boolean b1, Boolean b2 )
  {
    Boolean nullableValue = b1;
    if( nullableValue == null )
    {
      nullableValue = b2;
    }
    else if( b2 != null )
    {
      nullableValue &= b2;
    }
    return nullableValue;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    TypeAttributes that = (TypeAttributes)o;
    return Objects.equals( _nullable, that._nullable ) &&
           Objects.equals( _defaultValue, that._defaultValue );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _nullable, _defaultValue );
  }
}
