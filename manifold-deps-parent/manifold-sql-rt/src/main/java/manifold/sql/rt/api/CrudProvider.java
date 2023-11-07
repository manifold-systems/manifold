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

import java.sql.Connection;
import java.util.List;

public interface CrudProvider
{
  <T extends Entity> void create( Connection c, UpdateContext<T> ctx );
  <T extends Entity> T readOne( QueryContext<T> ctx );
  <T extends Entity> List<T> readMany( QueryContext<T> ctx );
  <T extends Entity> void update( Connection c, UpdateContext<T> ctx );
  <T extends Entity> void delete( Connection c, UpdateContext<T> ctx );
}
