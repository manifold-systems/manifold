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

import java.util.ArrayList;
import java.util.List;

public class JdbcForeignKeyData
{
  private final List<KeyPart> _keyParts;
  private final JdbcSchemaTable _table;

  public JdbcForeignKeyData( JdbcSchemaTable table, List<KeyPart> keyParts )
  {
    _table = table;
    _keyParts = keyParts;
  }

  public List<JdbcSchemaColumn> resolve( JdbcSchema schema )
  {
    List<JdbcSchemaColumn> foreignKeys = new ArrayList<>();
    for( KeyPart part : _keyParts )
    {
      JdbcSchemaColumn fromColumn = _table.getColumn( part.getFromColName() );
      JdbcSchemaTable toTable = schema.getTable( part.getToTableName() );
      JdbcSchemaColumn toColumn = toTable.getColumn( part.getToColName() );
      fromColumn.setForeignKey( toColumn );
      foreignKeys.add( fromColumn );
    }
    return foreignKeys;
  }

  static class KeyPart
  {
    private final String _fromColName;
    private final String _toColName;
    private final String _toTableName;

    public KeyPart( String fromColName, String toColName, String toTableName )
    {
      _fromColName = fromColName;
      _toColName = toColName;
      _toTableName = toTableName;
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
