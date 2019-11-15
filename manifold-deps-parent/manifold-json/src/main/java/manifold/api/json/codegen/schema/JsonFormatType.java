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

package manifold.api.json.codegen.schema;

import manifold.api.json.codegen.IJsonParentType;
import manifold.api.json.codegen.IJsonType;
import manifold.api.json.codegen.JsonBasicType;

/**
 * This type facilitates mapping a Java type to a JSON {@code "format"} type such as {@code "date-time}.
 * <p>
 * Implement the {@link IJsonFormatTypeResolver} service provider to map your own formats with Java types.
 */
public class JsonFormatType implements IJsonType
{
  private final String _format;
  private final Class<?> _javaType;
  private final TypeAttributes _typeAttributes;

  JsonFormatType( String format, Class<?> javaType )
  {
    this( format, javaType, new TypeAttributes() );
  }
  private JsonFormatType( String format, Class<?> javaType, TypeAttributes typeAttributes )
  {
    _format = format;
    _javaType = javaType;
    _typeAttributes = typeAttributes;
  }

  /**
   * The type that is generated as part of the JSON Java API.
   */
  @SuppressWarnings("WeakerAccess")
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
  public TypeAttributes getTypeAttributes()
  {
    return _typeAttributes;
  }
  @Override
  public IJsonType copyWithAttributes( TypeAttributes attributes )
  {
    if( getTypeAttributes().equals( attributes ) )
    {
      return this;
    }
    return new JsonFormatType( _format, _javaType, getTypeAttributes().overrideWith( attributes ) );
  }

  @Override
  public JsonFormatType merge( IJsonType type )
  {
    if( type instanceof JsonBasicType ||
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
