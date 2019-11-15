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

import java.util.List;
import javax.script.Bindings;
import manifold.ext.api.IListBacked;
import manifold.ext.api.IProxyFactory;
import manifold.ext.api.Structural;

/**
 * A base interface for all JSON and YAML types with methods to transform a JSON value List to/from JSON and YAML
 * and to conveniently use the List for JSON and YAML Web services / APIs.
 */
@Structural(factoryClass = IJsonList.Factory.class)
public interface IJsonList<T> extends IListBacked<T>
{
  /** Loader is a fluent API with methods for loading content from String, URL, file, etc. */
  static <T> Loader<IJsonList<T>> load()
  {
    return new Loader<>();
  }

  /** Writer is a fluent API to write this JSON object in various formats including JSON, YAML, and XML */
  default Writer write()
  {
    return new Writer( getList() );
  }

  /** Provides a deep copy of this list */
  default IJsonList<T> copy()
  {
    //noinspection unchecked
    return (IJsonList<T>)Bindings.deepCopyValue(getList(), DataBindings::new);
  }

  /** For Internal Use Only */
  class Factory implements IProxyFactory<List, IJsonList>
  {
    @Override
    public IJsonList proxy( List target, Class<IJsonList> iface )
    {
      return new JsonList( target );
    }
  }
}
