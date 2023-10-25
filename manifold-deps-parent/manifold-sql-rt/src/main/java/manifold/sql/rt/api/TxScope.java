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

import java.sql.SQLException;

/**
 * All data source entities have a data source transaction scope ({@code TxScope}). When an entity changes in application
 * code (created, updated, deleted) it is added to its tx scope. Entities manage their own state and keep track of changes.
 * Tx scopes manage persistence of changed state. The {@link #commit()} method persists changes to the data source
 * whenever needed and as often as needed.
 * <p/>
 * Note, if entity types and queries are not used with an explicit TxScope, a default scope is provided. The default scope
 * is a dependency, which can be customized.
 */
public interface TxScope
{
  /**
   * Provides data source configuration info for this tx scope.
   */
  DbConfig getDbConfig();

  /**
   * Commits entity changes in this tx scope to the data source specified in {@link #getDbConfig()}. After the commit,
   * this tx scope is ready to manage more entity changes. Note, if the commit fails, unless {@link #revert()} is called
   * during handling of the SQLException, the state of this tx scope is as it was immediately before the commit.
   * Otherwise, if the commit succeeds, the state of this tx scope is clear of changes.
   *
   * @throws SQLException
   */
  void commit() throws SQLException;

  /**
   * Reverts all entity changes within this tx scope back to the last commit, or if no commits were made, back to the
   * creation of this tx scope.
   *
   * <p/>
   * Note, this method is <i>not</i> called when a commit fails. The state of the scope is carefully managed so that
   * it is exactly as it was immediately before commit() was called. This behavior is intended to support another commit
   * attempt, if desired.
   * <p/>
   * However, catching the SQLException resulting from a failed commit is the place for the caller to decide whether to
   * revert or amend the changes in the tx scope.
   *
   * @throws SQLException
   */
  void revert() throws SQLException;
}
