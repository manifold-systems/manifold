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

import manifold.sql.rt.api.TableRow;
import manifold.sql.rt.api.TxScope;

import java.util.Set;

/**
 * This interface is for internal use.
 * <p/>
 * All TxScope SPI implementations must implement this interface.
 */
public interface OperableTxScope extends TxScope
{
  Set<TableRow> getRows();
  void addRow( TableRow item );
  void removeRow( TableRow item );
  boolean containsRow( TableRow item );
}
