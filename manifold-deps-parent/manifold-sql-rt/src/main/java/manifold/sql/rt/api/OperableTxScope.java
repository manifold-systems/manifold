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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This interface is for internal use.
 * <p/>
 * All TxScope SPI implementations must implement this interface.
 */
public interface OperableTxScope extends TxScope
{
  Set<Entity> getRows();
  void addRow( Entity item );
  void removeRow( Entity item );
  boolean containsRow( Entity item );
  Connection getActiveConnection();
  SqlChangeCtx newSqlChangeCtx( Connection c );
  BatchSqlChangeCtx newBatchSqlChangeCtx( Connection c );

  void addBatch( Executor exec, Consumer<Statement> consumer );

  // specific to duckdb (for now)
  <T extends SchemaAppender> void append( Consumer<T> consumer, T appender ) throws SQLException;
}
