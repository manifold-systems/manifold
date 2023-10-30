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

package manifold.ext.rt.api;

import manifold.rt.api.Bindings;

/**
 * A {@link Structural} interface with only getter/setter methods can extend this interface and provide default
 * implementations of its methods and implement a compile-time proxy API to avoid the overhead runtime proxy
 * generation.
 * <p/>
 * See the {@code JsonStructureType}.
 */
public interface IBindingsBacked
{
  /**
   * The {@link Bindings} object used to store name/value pairs corresponding with getter/setter methods.
   */
  Bindings getBindings();

  default String display()
  {
    return getBindings().displayEntries();
  }
}
