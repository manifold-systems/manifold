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

package manifold.sql.schema.jdbc;

import manifold.rt.api.util.Pair;
import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.api.SchemaForeignKey;
import manifold.sql.schema.api.SchemaTable;

import java.util.*;

public class JdbcForeignKeyMetadata
{
  private final JdbcSchemaTable _table;
  private final List<KeyPart> _keyParts;

  public JdbcForeignKeyMetadata( JdbcSchemaTable table, List<KeyPart> keyParts )
  {
    _table = table;
    _keyParts = keyParts;
  }

  public Map<SchemaTable, List<SchemaForeignKey>> resolve( JdbcSchema schema )
  {
    Map<SchemaTable, List<SchemaForeignKey>> foreignKeys = new LinkedHashMap<>();
    Map<Pair<String, SchemaTable>, List<SchemaColumn>> foreignKeyColumns = new LinkedHashMap<>();
    for( KeyPart part : _keyParts )
    {
      SchemaColumn fromColumn = _table.getColumn( part.getFromColName() );
      SchemaTable toTable = schema.getTable( part.getToTableName() );
      SchemaColumn toColumn = toTable.getColumn( part.getToColName() );
      ((JdbcSchemaColumn)fromColumn).setForeignKey( (JdbcSchemaColumn)toColumn );
      String fkName = part.getFkName();
      foreignKeyColumns.computeIfAbsent( new Pair<>( fkName, toTable ), __ -> new ArrayList<>() )
          .add( fromColumn );
    }

    foreignKeyColumns.forEach( (k, v) -> foreignKeys.computeIfAbsent( k.getSecond(), __ -> new ArrayList<>() )
      .add( new JdbcSchemaForeignKey( k.getFirst(), k.getSecond(), v ) ) );
    return foreignKeys;
  }

  static class KeyPart
  {
    private final String _fkName;
    private final String _fromColName;
    private final String _toColName;
    private final String _toTableName;

    public KeyPart( String fkName, String fromColName, String toColName, String toTableName )
    {
      _fkName = fkName;
      _fromColName = fromColName;
      _toColName = toColName;
      _toTableName = toTableName;
    }

    public String getFkName()
    {
      return _fkName;
    }

    public String getFromColName()
    {
      return _fromColName;
    }

    public String getToColName()
    {
      return _toColName;
    }

    public String getToTableName()
    {
      return _toTableName;
    }
  }
}
