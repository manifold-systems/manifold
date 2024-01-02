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

import manifold.rt.api.Bindings;

import java.sql.SQLException;
import java.util.Map;

public interface OperableTxBindings extends TxBindings
{
  @SuppressWarnings( "unused" )
  void setOwner( Entity owner );

  void setDelete( boolean value );

  void holdValues( Bindings generatedKeys );
  void holdValue( String name, Object value );
  Object getHeldValue( String name );
  void dropHeldValues();

  void commit() throws SQLException;
  void failedCommit() throws SQLException;
  void revert() throws SQLException;

  Map<String, Object> persistedStateEntrySet();
  Map<String, Object> uncommittedChangesEntrySet();

  Object getPersistedStateValue( String name );

  void reuse();
}
