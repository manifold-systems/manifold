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
import manifold.sql.rt.util.DriverInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static manifold.sql.rt.util.DriverInfo.Oracle;

public class JdbcSchemaTable implements SchemaTable
{
  private static final Logger LOGGER = LoggerFactory.getLogger( JdbcSchemaTable.class );

  private final JdbcSchema _schema;
  private final String _name;
  private final String _description;
  private final Kind _kind;
  private final Map<String, SchemaColumn> _columns;
  private final JdbcSchemaColumn _nonNullUniqueId;
  private final ArrayList<SchemaColumn> _primaryKeys;
  private final Map<String, List<SchemaColumn>> _nonNullUniqueKeys; // does not include pk
  private final JdbcForeignKeyMetadata _foreignKeyData;
  private final Map<SchemaTable, List<SchemaForeignKey>> _foreignKeys;
  private final Set<SchemaForeignKey> _oneToMany;
  private final Set<Pair<SchemaColumn, SchemaColumn>> _manyToMany;

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

    Map<String, Set<String>> uniqueKeys = new LinkedHashMap<>();
    try( ResultSet indexInfo = metaData.getIndexInfo( catalogName, schemaName, _name, true, true ) )
    {
      while( indexInfo.next() )
      {
        if( !indexInfo.getBoolean( "NON_UNIQUE" ) )
        {
          String indexName = indexInfo.getString( "INDEX_NAME" );
          if( indexName != null ) // sql server always includes a null index for some reason
          {
            uniqueKeys.computeIfAbsent( indexName, __ -> new LinkedHashSet<>() )
              .add( indexInfo.getString( "COLUMN_NAME" ) );
          }
        }
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
    _foreignKeys = new LinkedHashMap<>();
    _nonNullUniqueKeys = new LinkedHashMap<>();
    _oneToMany = new LinkedHashSet<>();
    _manyToMany = new LinkedHashSet<>();
    List<String> columnClassNames = getColumnClassNames( metaData );

    DriverInfo driver = DriverInfo.lookup( metaData );
    if( schemaName != null && !schemaName.isEmpty() && driver == Oracle )
    {
      // there is a bug in oracle driver where metaData.getColumns() fails if the schema is set to anything other than
      // the logged-in user, so we set that here. We reset it back in the finally block.
      metaData.getConnection().setSchema( metaData.getUserName() );
    }

    try( ResultSet colResults = metaData.getColumns( catalogName, schemaName, _name, null ) )
    {
      int i = 0;
      JdbcSchemaColumn id = null;
      while( colResults.next() )
      {
        i++;
        JdbcSchemaColumn col = new JdbcSchemaColumn( i, this, colResults, primaryKey, uniqueKeys, columnClassNames.get( i-1 ), metaData );
        _columns.put( col.getName(), col );
        if( col.isNonNullUniqueId() )
        {
          if( id == null || id.isPrimaryKeyPart() )
          {
            // if there is a pk, ensure that is the id, otherwise first non-null unique key is the id
            id = col;
          }
        }
        if( col.isPrimaryKeyPart() )
        {
          _primaryKeys.add( col );
        }

        buildNonNullUniqueKeys( col );
      }
      _nonNullUniqueId = id;
    }
    finally
    {
      if( schemaName != null && !schemaName.isEmpty() && driver == Oracle )
      {
        // set the schema back to the configured schema
        metaData.getConnection().setSchema( schemaName );
      }
    }
  }

  @NotNull
  private List<String> getColumnClassNames( DatabaseMetaData metaData ) throws SQLException
  {
    List<String> columnClassNames = new ArrayList<>();
    try( PreparedStatement preparedStatement = metaData.getConnection().prepareStatement( "select * from " + _name ) )
    {
      int columnCount = preparedStatement.getMetaData().getColumnCount();
      for( int i = 0; i < columnCount; i++ )
      {
        try
        {
          columnClassNames.add( preparedStatement.getMetaData().getColumnClassName( i + 1 ) );
        }
        catch( SQLException se )
        {
          LOGGER.warn( "getColumnClassName() failed.", se );
          columnClassNames.add( Object.class.getName() );
        }
      }
    }
    return columnClassNames;
  }

  private void buildNonNullUniqueKeys( JdbcSchemaColumn col )
  {
    String nonNullUniqueKeyName = col.getNonNullUniqueKeyName();
    if( nonNullUniqueKeyName != null )
    {
      _nonNullUniqueKeys.computeIfAbsent( nonNullUniqueKeyName, __ -> new ArrayList<>() )
        .add( col );
    }

    // now remove nullable keys and the pk, we want non-null keys other than the pk in this map

    Set<String> removeKeys = new HashSet<>();
    for( String keyName : _nonNullUniqueKeys.keySet() )
    {
      List<SchemaColumn> cols = _nonNullUniqueKeys.get( keyName );
      for( SchemaColumn schemaColumn : cols )
      {
        if( schemaColumn.isNullable() )
        {
          // remove nullable keys
          removeKeys.add( keyName );
          break;
        }
      }

      if( !removeKeys.contains( keyName ) &&
        cols.stream().allMatch( c -> c.isPrimaryKeyPart() ) )
      {
        // don't want the pk in this map
        removeKeys.add( keyName );
      }
    }
    removeKeys.forEach( key -> _nonNullUniqueKeys.remove( key ) );
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
    return _nonNullUniqueId;
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
  public Map<String, List<SchemaColumn>> getNonNullUniqueKeys()
  {
    return _nonNullUniqueKeys;
  }

  @Override
  public String getDescription()
  {
    return _description;
  }

  @Override
  public void resolveForeignKeys()
  {
    // resolve foreign keys
    _foreignKeys.putAll( _foreignKeyData.resolve( _schema ) );
  }

  @Override
  public void resolveFkRelations()
  {
    // resolve one-to-many, many-to-many
    for( List<SchemaForeignKey> fkDefs : _foreignKeys.values() )
    {
      for( SchemaForeignKey fkDef : fkDefs )
      {
        JdbcSchemaTable referencedTable = (JdbcSchemaTable)fkDef.getReferencedTable();
        List<SchemaColumn> fkColumns = fkDef.getColumns();
        if( fkColumns.stream().noneMatch( c -> c.isNonNullUniqueId() ) ) // uniqueness implies _not_ an x-to-many
        {
          // infer fk as many-to-many if fk is part of non-null unique key and all the key's cols are fk parts
          Pair<SchemaColumn, SchemaColumn> manyToManyKey = getManyToManyKey( fkDef );
          if( manyToManyKey != null )
          {
            referencedTable._manyToMany.add( manyToManyKey );
          }
          else
          {
            referencedTable._oneToMany.add( fkDef );
          }
        }
      }
    }
  }

  private Pair<SchemaColumn, SchemaColumn> getManyToManyKey( SchemaForeignKey fkDef )
  {
    List<SchemaColumn> pk = getPrimaryKey();
    if( isManyToMany( fkDef, pk ) )
    {
      return new Pair<>( pk.get( 0 ), pk.get( 1 ) );
    }
    for( List<SchemaColumn> ukColumns : getNonNullUniqueKeys().values() )
    {
      if( isManyToMany( fkDef, ukColumns ) )
      {
        return new Pair<>( ukColumns.get( 0 ), ukColumns.get( 1 ) );
      }
    }
    return null;
  }

  private boolean isManyToMany( SchemaForeignKey sfk, List<SchemaColumn> pkColumns )
  {
    List<SchemaColumn> fkColumns = sfk.getColumns();
    //noinspection SlowListContainsAll
    return (pkColumns.size() > fkColumns.size() && pkColumns.containsAll( fkColumns ) &&
      pkColumns.stream().allMatch( c -> c.getForeignKey() != null )
    // following conditions narrow many-to-many to cases involving two fks having only one column and
    // where both fks must reference difference tables to avoid issues with self referencing many-to-many relationships.
    && pkColumns.size() == 2 &&
      pkColumns.get( 0 ).getForeignKey().getTable() != pkColumns.get( 1 ).getForeignKey().getTable());// this check for size==2 is just here to simplify many to many
  }

  public Set<SchemaForeignKey> getOneToMany()
  {
    return _oneToMany;
  }

  public Set<Pair<SchemaColumn, SchemaColumn>> getManyToMany()
  {
    return _manyToMany;
  }

  public List<SchemaColumn> getNonNullColumns()
  {
    return getColumns().values().stream()
      .filter( c -> !c.isNullable() )
      .collect( Collectors.toList() );
  }
}
