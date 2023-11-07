/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.rt.api;

/**
 * Generated "Entity" classes from SchemaParentType extend this base class. Custom classes via {@link CustomEntityFactory}
 * must subclass this as well, or if a different super class is needed, the subclass must duplicate the behavior here.
 */
public abstract class BaseEntity implements ResultRow
{
  private final TxBindings _txBindings;

  public BaseEntity( TxBindings txBindings )
  {
    _txBindings = txBindings;
  }

  @Override
  public TxBindings getBindings()
  {
    return _txBindings;
  }

  public String toString()
  {
    return "{" + getBindings().displayEntries() + "}";
  }

  @Override
  public int hashCode()
  {
    return getBindings().hashCode();
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
    BaseEntity that = (BaseEntity)o;
    return getBindings().equals( that.getBindings() );
  }
}
