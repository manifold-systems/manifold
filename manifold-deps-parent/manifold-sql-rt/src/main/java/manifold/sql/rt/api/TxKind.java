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

public enum TxKind
{
  /**
   * Changes apply to SQL Insert.
   */
  Insert,

  /**
   * Changes apply to SQL Update.
   * <p/>
   * This state exists exclusively during a live transaction and within a {@link TxScope.ScopeConsumer} when an entity is
   * Inserted via CRUD and then further modified via CRUD after the Insert. Such modifications are persisted via Update.
   * <p/>
   * Example:<pre><code>
   * Sakila.commit(ctx -> {
   *   Film film = Film...build(); // txbindings state is now Insert
   *   film.setRating(...);
   *   List&lt;Film&gt; films = Film.fetch(...); // query triggers prior CRUD to persist, INSERT film
   *   film.setDescription(...); // txbindings state is now InsertUpdate
   *   film.setSomethingElse(...);
   *   List&lt;Inventory&gt; inv = Inventory.fetc(...); // query triggers prior CRUD to persist, UPDATE film
   *   ...
   * });
   * </code>
   * </pre>
   *
   */
  InsertUpdate,

  /**
   * Changes apply to SQL Update.
   */
  Update,

  /**
   * Nothing is known about changes.
   */
  Unknown
}
