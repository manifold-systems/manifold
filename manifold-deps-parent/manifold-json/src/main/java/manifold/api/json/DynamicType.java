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

import manifold.api.json.schema.TypeAttributes;

/**
 */
public class DynamicType implements IJsonType
{
  private static final DynamicType INSTANCE = new DynamicType();

  public static DynamicType instance()
  {
    return INSTANCE;
  }

  private final TypeAttributes _typeAttributes;

  private DynamicType()
  {
    _typeAttributes = new TypeAttributes( true, null );
  }

  @Override
  public String getName()
  {
    return "Dynamic";
  }

  @Override
  public String getIdentifier()
  {
    return "Object";
  }

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
    return this;
  }

  @Override
  public IJsonType merge( IJsonType type )
  {
    return null;
  }
}
