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

package manifold.sql.query.jdbc;

import manifold.sql.api.Table;
import manifold.sql.query.api.QueryColumn;
import manifold.sql.schema.api.SchemaTable;
import manifold.sql.schema.jdbc.JdbcSchemaTable;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class JdbcQueryColumn implements QueryColumn
{
  private final JdbcQueryTable _queryTable;
  private final JdbcSchemaTable _schemaTable;
  private final int _position;
  private final String _name;
  private final String _schemaName;
  private final Class<?> _type;
  private final int _size;
  private final int _scale;
  private final int _displaySize;
  private final boolean _isNullable;
  private final boolean _isCurrency;
  private final boolean _isReadOnly;
  private final boolean _isSigned;

  public JdbcQueryColumn( int colIndex, JdbcQueryTable queryTable, ResultSetMetaData rsMetaData ) throws SQLException
  {
    _position = colIndex;
    _queryTable = queryTable;

    String tableName = rsMetaData.getTableName( colIndex );
    _schemaTable = tableName == null || tableName.isEmpty()
      ? null // null if query column is not a table column eg. calculated
      : (JdbcSchemaTable)_queryTable.getSchema().getTable( tableName );

    _name = rsMetaData.getColumnLabel( colIndex );
    _schemaName = rsMetaData.getColumnName( colIndex );

    int typeId = rsMetaData.getColumnType( colIndex );
    _type = queryTable.getTypeMap().getType( this, typeId );
    if( _type == null )
    {
      throw new IllegalStateException( "Type is null for query: " + _queryTable.getName() + " column:" + _name );
    }

    _isNullable = rsMetaData.isNullable( colIndex ) != ResultSetMetaData.columnNoNulls;

    _size = rsMetaData.getPrecision( colIndex );
    _scale = rsMetaData.getScale( colIndex );

    _displaySize = rsMetaData.getColumnDisplaySize( colIndex );
    _isCurrency = rsMetaData.isCurrency( colIndex );
    _isReadOnly = rsMetaData.isReadOnly( colIndex );
    _isSigned = rsMetaData.isSigned( colIndex );
  }

  @Override
  public Table getTable()
  {
    return _queryTable;
  }

  public SchemaTable getSchemaTable()
  {
    return _schemaTable;
  }

  @Override
  public int getPosition()
  {
    return _position;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public Class<?> getType()
  {
    return _type;
  }

  @Override
  public boolean isNullable()
  {
    return _isNullable;
  }

  @Override
  public int getSize()
  {
    return _size;
  }

  public int getScale()
  {
    return _scale;
  }

  public JdbcQueryTable getQueryTable()
  {
    return _queryTable;
  }

  public String getSchemaName()
  {
    return _schemaName;
  }

  public int getDisplaySize()
  {
    return _displaySize;
  }

  public boolean isCurrency()
  {
    return _isCurrency;
  }

  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  public boolean isSigned()
  {
    return _isSigned;
  }
}
