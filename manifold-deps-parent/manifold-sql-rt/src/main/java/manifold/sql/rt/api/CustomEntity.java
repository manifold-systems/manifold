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
 * A base interface for custom entity interfaces. Provides type-safe access to the extended interface via {@link #self()}.
 * @param <E> The entity interface to customize
 */
public interface CustomEntity<E extends Entity & CustomEntity<E>>
{
  /**
   * Provides type-safe access to the customized entity interface.
   * @return The instance of the entity interface.
   */
  default E self() {
    //noinspection unchecked
    return (E)this;
  }
}
