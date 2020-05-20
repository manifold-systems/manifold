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

public class JsonList<T> implements IJsonList<T>
{
  private final List _list;

  public JsonList()
  {
    _list = new ArrayList<>();
  }

  public JsonList( List list )
  {
    _list = list;
  }

  @Override
  public List<T> getList()
  {
    return _list;
  }
}
