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

import java.util.Set;

public interface TxBindings extends Bindings
{
  TableRow getOwner();
  void setOwner( TableRow owner );

  TxScope getBinder();

  boolean isForInsert();
  boolean isForUpdate();
  boolean isForDelete();

  void setDelete( boolean value );

  void holdValues( Bindings generatedKeys );
  void dropHeldValues();

  void commit();

  Set<Entry<String, Object>> changedEntrySet();
  Set<Entry<String, Object>> initialStateEntrySet();

  Object getInitialValue( String name );
}
