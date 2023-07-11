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

import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.api.SchemaForeignKey;
import manifold.sql.schema.api.SchemaTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JdbcSchemaTable implements SchemaTable
{
  private final JdbcSchema _schema;
  private final String _name;
  private final String _description;
  private final Kind _kind;
  private final Map<String, SchemaColumn> _columns;
  private final JdbcSchemaColumn _colId;
  private final ArrayList<SchemaColumn> _primaryKeys;
  private final JdbcForeignKeyMetadata _foreignKeyData;
  private Map<SchemaTable, List<SchemaForeignKey>> _foreignKeys;


  public JdbcSchemaTable( JdbcSchema owner, DatabaseMetaData metaData, ResultSet resultSet ) throws SQLException
  {
    _schema = owner;
    _name = resultSet.getString( "TABLE_NAME" );
    _description = resultSet.getString( "REMARKS" );
    _kind = Kind.get( resultSet.getString( "TABLE_TYPE" ) );
    if( _kind == null )
    {
      throw new IllegalStateException( "Unexpected table kind for: " + _name );
    }

    List<String> primaryKey = new ArrayList<>();
    String catalogName = _schema.getDbConfig().getCatalogName();
    String schemaName = _schema.getName();
    try( ResultSet primaryKeys = metaData.getPrimaryKeys( catalogName, schemaName, _name ) )
    {
      while( primaryKeys.next() )
      {
        String columnName = primaryKeys.getString( "COLUMN_NAME" );
        primaryKey.add( columnName );
      }
    }

    try( ResultSet foreignKeys = metaData.getImportedKeys( catalogName, schemaName, _name ) )
    {
      List<JdbcForeignKeyMetadata.KeyPart> keyParts = new ArrayList<>();
      while( foreignKeys.next() )
      {
        String fkName = foreignKeys.getString( "FK_NAME" );
        String fkColumnName = foreignKeys.getString( "FKCOLUMN_NAME" );
        String pkColumnName = foreignKeys.getString( "PKCOLUMN_NAME" );
        String pkTableName = foreignKeys.getString( "PKTABLE_NAME" );
        keyParts.add( new JdbcForeignKeyMetadata.KeyPart( fkName, fkColumnName, pkColumnName, pkTableName ) );
      }
      _foreignKeyData = new JdbcForeignKeyMetadata( this, keyParts );
    }

    _columns = new LinkedHashMap<>();
    _primaryKeys = new ArrayList<>();
    try( ResultSet colResults = metaData.getColumns( catalogName, schemaName, _name, null ) )
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
  public Map<String, SchemaColumn> getColumns()
  {
    return _columns;
  }

  @Override
  public SchemaColumn getColumn( String columnName )
  {
    return _columns.get( columnName );
  }

  @Override
  public JdbcSchemaColumn getId()
  {
    return _colId;
  }

  @Override
  public Map<SchemaTable, List<SchemaForeignKey>> getForeignKeys()
  {
    return _foreignKeys;
  }

  @Override
  public List<SchemaColumn> getPrimaryKey()
  {
    return _primaryKeys;
  }

  @Override
  public String getDescription()
  {
    return _description;
  }

  @Override
  public void resolve()
  {
    // resolve foreign keys
    _foreignKeys = _foreignKeyData.resolve( _schema );
  }

  public List<SchemaColumn> getNonNullColumns()
  {
    return getColumns().values().stream()
      .filter( c -> !c.isNullable() )
      .collect( Collectors.toList() );
  }
}
