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

import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonSimpleType;
import manifold.api.json.JsonSimpleTypeWithDefault;

/**
 * This type facilitates mapping a Java type to a JSON {@code "format"} type such as {@code "date-time}.
 * <p>
 * Implement the {@link IJsonFormatTypeResolver} service provider to map your own formats with Java types.
 */
public class JsonFormatType implements IJsonType
{
  private final String _format;
  private final Class<?> _javaType;
  private Object _defaultValue;

  JsonFormatType( String format, Class<?> javaType )
  {
    _format = format;
    _javaType = javaType;
  }

  /**
   * The type that is generated as part of the JSON Java API.
   */
  public Class<?> getJavaType()
  {
    return _javaType;
  }

  public String getFormat()
  {
    return _format;
  }

  @Override
  public String getName()
  {
    return _format;
  }

  /**
   * Format types never have a parent.
   */
  @Override
  public IJsonParentType getParent()
  {
    return null;
  }

  @Override
  public Object getDefaultValue()
  {
    return _defaultValue;
  }
  @Override
  public IJsonType setDefaultValue( Object value )
  {
    _defaultValue = value;
    return this;
  }

  @Override
  public JsonFormatType merge( IJsonType type )
  {
    if( type instanceof JsonSimpleType ||
        type instanceof JsonSimpleTypeWithDefault ||
        type instanceof JsonFormatType )
    {
      //## todo: maybe be smarter about merging two format types?
      return this;
    }
    return null;
  }

  /**
   * The identifier must be the name of the class used in the generated JSON API.
   */
  @Override
  public String getIdentifier()
  {
    return getJavaType().getTypeName();
  }
}
