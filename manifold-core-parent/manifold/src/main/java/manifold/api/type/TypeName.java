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

package manifold.api.type;

import manifold.api.host.IModule;

public class TypeName implements Comparable
{
  public final String name;
  public final Kind kind;
  public final Visibility visibility;
  public final IModule module;

  public TypeName( String name, IModule module, Kind kind, Visibility visibility )
  {
    this.name = name;
    this.module = module;
    this.kind = kind;
    this.visibility = visibility;
  }

  @Override
  public int compareTo( Object o )
  {
    return -(kind.ordinal() - ((TypeName)o).kind.ordinal());
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

    TypeName typeName = (TypeName)o;

    if( kind != typeName.kind )
    {
      return false;
    }
    if( module != null ? !module.equals( typeName.module ) : typeName.module != null )
    {
      return false;
    }
    if( name != null ? !name.equals( typeName.name ) : typeName.name != null )
    {
      return false;
    }
    if( visibility != typeName.visibility )
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (kind != null ? kind.hashCode() : 0);
    result = 31 * result + (visibility != null ? visibility.hashCode() : 0);
    result = 31 * result + (module != null ? module.hashCode() : 0);
    return result;
  }

  public IModule getModule()
  {
    return module;
  }

  public enum Kind
  {
    TYPE,
    NAMESPACE
  }

  public enum Visibility
  {
    PUBLIC,
    PROTECTED,
    PRIVATE,
  }

  @Override
  public String toString()
  {
    return kind + " " + name + ": " + visibility;
  }
}
