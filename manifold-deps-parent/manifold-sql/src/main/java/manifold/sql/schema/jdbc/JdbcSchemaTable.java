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
import manifold.sql.query.type.SqlIssueContainer;
import manifold.sql.rt.util.DbUtil;
import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.api.SchemaForeignKey;
import manifold.sql.schema.api.SchemaTable;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.schema.jdbc.oneoff.DuckDbForeignKeys;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static manifold.sql.rt.util.DriverInfo.DuckDB;
import static manifold.sql.rt.util.DriverInfo.Oracle;

public class JdbcSchemaTable implements SchemaTable
{
  private static final Logger LOGGER = LoggerFactory.getLogger( JdbcSchemaTable.class );

  private final JdbcSchema _schema;
  private final String _name;
  private final String _escapedName;
  private final String _description;
  private final String _tableDdl;
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
    _escapedName =  DbUtil.enquoteIdentifier(_name, metaData);

    List<String> primaryKey = new ArrayList<>();
    String schemaName = _schema.getName();
    String catalogName = _schema.isCatalogBased() ? schemaName : _schema.getDbConfig().getCatalogName();
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
    catch( SQLFeatureNotSupportedException ignore )
    {
      // getIndexInfo not supported e.g., DuckDB
    }

    List<JdbcForeignKeyMetadata.KeyPart> keyParts = new ArrayList<>();
    try( ResultSet foreignKeys = _schema.getDriverInfo() == DuckDB
                                 ? DuckDbForeignKeys.getImportedKeys( metaData, catalogName, schemaName, _name )
                                 : metaData.getImportedKeys( catalogName, schemaName, _name ) )
    {
      while( foreignKeys.next() )
      {
        String fkName = foreignKeys.getString( "FK_NAME" );
        String fkColumnName = foreignKeys.getString( "FKCOLUMN_NAME" );
        String pkColumnName = foreignKeys.getString( "PKCOLUMN_NAME" );
        String pkTableName = foreignKeys.getString( "PKTABLE_NAME" );
        if( _schema.hasTable( pkTableName ) ) // prevent FK refs to ALIAS and SYNONYM tables, only allowing direct table FKs, for now
        {
          keyParts.add( new JdbcForeignKeyMetadata.KeyPart( fkName, fkColumnName, pkColumnName, pkTableName ) );
        }
      }
    }
    catch( SQLFeatureNotSupportedException ignore )
    {
      // getImportedKeys not supported
    }
    _foreignKeyData = new JdbcForeignKeyMetadata( this, keyParts );

    _columns = new LinkedHashMap<>();
    _primaryKeys = new ArrayList<>();
    _foreignKeys = new LinkedHashMap<>();
    _nonNullUniqueKeys = new LinkedHashMap<>();
    _oneToMany = new LinkedHashSet<>();
    _manyToMany = new LinkedHashSet<>();
    _tableDdl = null; // todo: generate DDL from metadata
    Map<String, String> columnClassNames = getColumnClassNames( metaData );

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
        String columnClassName = columnClassNames.get( colResults.getString( "COLUMN_NAME" ) );
        JdbcSchemaColumn col = new JdbcSchemaColumn( i, this, colResults, primaryKey, uniqueKeys, columnClassName, metaData );
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
  private Map<String, String> getColumnClassNames( DatabaseMetaData metaData ) throws SQLException
  {
    Map<String, String> columnClassNames = new LinkedHashMap<>();
    try( PreparedStatement preparedStatement = metaData.getConnection().prepareStatement("select * from " + _escapedName) )
    {
      int columnCount = preparedStatement.getMetaData().getColumnCount();
      for( int i = 0; i < columnCount; i++ )
      {
        try
        {
          String columnName = preparedStatement.getMetaData().getColumnName( i + 1 );
          String columnClassName = preparedStatement.getMetaData().getColumnClassName( i + 1 );
          columnClassName = tailorIfNecessary( columnClassName );
          columnClassNames.put( columnName, columnClassName );
        }
        catch( SQLException se )
        {
          LOGGER.warn( "getColumnClassName() failed for table '" + _escapedName + "' column #" + (i + 1), se );
        }
      }
    }
    return columnClassNames;
  }

  private String tailorIfNecessary( String columnClassName )
  {
    try
    {
      Class<?> cls = Class.forName( columnClassName );
      if( SQLXML.class.isAssignableFrom( cls ) )
      {
        // driver-specific XML types suck
        columnClassName = SQLXML.class.getTypeName();
      }
    }
    catch( ClassNotFoundException ignore )
    {
    }
    return columnClassName;
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
  public String getEscapedName()
  {
    return _escapedName;
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
  public String getSqlSource()
  {
    return _tableDdl;
  }

  @Override
  public SqlIssueContainer getIssues()
  {
    return null;
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
      pkColumns.get( 0 ).getForeignKey().getOwner() != pkColumns.get( 1 ).getForeignKey().getOwner());// this check for size==2 is just here to simplify many to many
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
