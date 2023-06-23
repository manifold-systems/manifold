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

import manifold.sql.api.Column;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcSchemaTable implements SchemaTable
{
  private final JdbcSchema _schema;
  private final String _name;
  private final String _description;
  private final Kind _kind;
  private final Map<String, JdbcSchemaColumn> _columns;
  private final JdbcSchemaColumn _colId;
  private final ArrayList<JdbcSchemaColumn> _primaryKeys;
  private final JdbcForeignKeyData _foreignKey;
  private List<JdbcSchemaColumn> _foreignKeys;


  public JdbcSchemaTable( Connection c, Schema owner, DatabaseMetaData metaData, ResultSet resultSet ) throws SQLException
  {
    _schema = (JdbcSchema)owner;
    _name = resultSet.getString( "TABLE_NAME" );
    _description = resultSet.getString( "REMARKS" );
    _kind = Kind.get( resultSet.getString( "TABLE_TYPE" ) );
    if( _kind == null )
    {
      throw new IllegalStateException( "Unexpected table kind for: " + _name );
    }

    List<String> primaryKey = new ArrayList<>();
    try( ResultSet primaryKeys = metaData.getPrimaryKeys( null, null, _name ) )
    {
      while( primaryKeys.next() )
      {
        String columnName = primaryKeys.getString( "COLUMN_NAME" );
        primaryKey.add( columnName );
      }
    }

    try( ResultSet foreignKeys = metaData.getImportedKeys( null, null, _name ) )
    {
      List<JdbcForeignKeyData.KeyPart> keyParts = new ArrayList<>();
      while( foreignKeys.next() )
      {
        String fkColumnName = foreignKeys.getString( "FKCOLUMN_NAME" );
        String pkColumnName = foreignKeys.getString( "PKCOLUMN_NAME" );
        String pkTableName = foreignKeys.getString( "PKTABLE_NAME" );
        keyParts.add( new JdbcForeignKeyData.KeyPart( fkColumnName, pkColumnName, pkTableName ) );
      }
      _foreignKey = new JdbcForeignKeyData( this, keyParts );
    }

    _columns = new LinkedHashMap<>();
    _primaryKeys = new ArrayList<>();
    try( ResultSet colResults = metaData.getColumns( null, null, _name, null ) )
    {
      int i = 0;
      JdbcSchemaColumn id = null;
      while( colResults.next() )
      {
        JdbcSchemaColumn col = new JdbcSchemaColumn( ++i, this, colResults, primaryKey );
        _columns.put( col.getName(), col );
        if( col.isId() )
        {
          if( id != null )
          {
            throw new IllegalStateException();
          }
          id = col;
        }
        if( col.isPrimaryKeyPart() )
        {
          _primaryKeys.add( col );
        }
      }
      _colId = id;
    }
  }

  @Override
  public JdbcSchema getSchema()
  {
    return _schema;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public Kind getKind()
  {
    return _kind;
  }

  @Override
  public Map<String, JdbcSchemaColumn> getColumns()
  {
    return _columns;
  }

  @Override
  public JdbcSchemaColumn getColumn( String columnName )
  {
    return _columns.get( columnName );
  }

  @Override
  public Column getId()
  {
    return _colId;
  }

  @Override
  public List<Column> getForeignKeys()
  {
    //noinspection unchecked
    return (List)_foreignKeys;
  }

  @Override
  public List<Column> getPrimaryKey()
  {
    //noinspection unchecked
    return (List)_primaryKeys;
  }

  void resolve()
  {
    // resolve foreign keys
    List<JdbcSchemaColumn> keys = _foreignKey.resolve( _schema );
    _foreignKeys = keys;
  }
}
