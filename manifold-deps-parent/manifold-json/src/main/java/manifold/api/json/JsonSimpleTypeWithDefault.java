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

package manifold.api.json;

import manifold.api.json.schema.JsonFormatType;

public class JsonSimpleTypeWithDefault implements IJsonType
{
  private final JsonSimpleType _delegate;
  private Object _defaultValue;

  JsonSimpleTypeWithDefault( JsonSimpleType delegate, Object defaultValue )
  {
    _delegate = delegate;
    _defaultValue = defaultValue;
  }

  @Override
  public String getName()
  {
    return _delegate.getName();
  }

  @Override
  public String getIdentifier()
  {
    return _delegate.getIdentifier();
  }

  @Override
  public IJsonParentType getParent()
  {
    return _delegate.getParent();
  }

  @Override
  public Object getDefaultValue()
  {
    return _defaultValue;
  }
  public JsonSimpleTypeWithDefault setDefaultValue( Object value )
  {
    _defaultValue = value;
    return this;
  }

  @Override
  public IJsonType merge( IJsonType other )
  {
    if( other instanceof JsonSimpleType )
    {
      IJsonType mergedType = _delegate.merge( other );
      if( mergedType != null )
      {
        return mergedType.setDefaultValue( getDefaultValue() );
      }
    }
    else if( other instanceof JsonSimpleTypeWithDefault )
    {
      IJsonType mergedType = _delegate.merge( ((JsonSimpleTypeWithDefault)other)._delegate );
      if( mergedType != null )
      {
        return mergedType.setDefaultValue( getDefaultValue() );
      }
    }
    else if( other instanceof JsonFormatType )
    {
      return other;
    }
    return null;
  }
}
