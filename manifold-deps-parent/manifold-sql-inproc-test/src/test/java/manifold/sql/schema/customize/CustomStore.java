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

package manifold.sql.schema.customize;

import manifold.sql.rt.api.CustomEntity;
import manifold.sql.schema.simple.h2.H2Sakila.Store;

/**
 * Following the "Custom" + [entity interface name] naming convention, this interface becomes a super interface for Store.
 * As such, methods defined here are accessible directly from instances of Store.
 */
public interface CustomStore extends CustomEntity<Store>
{
  default long myCustomMethod()
  {
    return self().getStoreId();
  }
}
