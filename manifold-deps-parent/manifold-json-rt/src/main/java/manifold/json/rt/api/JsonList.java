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

package manifold.json.rt.api;

import manifold.json.rt.api.IJsonList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonList<T> implements IJsonList<T>
{
  private final List _list;
  private final Class<T> _finalComponentType;

  public JsonList( Class<T> finalComponentType )
  {
    _list = new ArrayList<>();
    _finalComponentType = finalComponentType;
  }

  public JsonList( List jsonList, Class<T> finalComponentType )
  {
    _list = jsonList;
    _finalComponentType = finalComponentType;
  }

  @Override
  public List getList()
  {
    return _list;
  }

  @Override
  public Class<?> getFinalComponentType()
  {
    return _finalComponentType;
  }

  @Override
  public boolean equals( Object o )
  {
    return Objects.equals( _list, o );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _list );
  }

  @Override
  public String toString()
  {
    return _list.toString();
  }
}
