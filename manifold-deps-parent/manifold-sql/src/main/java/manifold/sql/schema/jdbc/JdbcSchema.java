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

import manifold.rt.api.util.ManIdentifierUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaTable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;
import static manifold.sql.rt.util.DriverInfo.Oracle;

public class JdbcSchema implements Schema
{
  private static final Logger LOGGER = LoggerFactory.getLogger( JdbcSchema.class );

  private static final List<String> TABLE_TYPES = Arrays.asList( "TABLE", "BASE TABLE", "VIEW" );

  private final String _name;
  private final DbConfig _dbConfig;
  private final Map<String, SchemaTable> _tables;
  private final Map<String, String> _javaToName;
  private final Map<String, String> _nameToJava;
  private final DriverInfo _driverInfo;
  private final boolean _schemaIsCatalog;
  private boolean _hasSchemas;
  private boolean _hasCatalogs;

  public JdbcSchema( DbConfig dbConfig ) throws SQLException
  {
    _dbConfig = dbConfig;
    _tables = new LinkedHashMap<>();
    _javaToName = new LinkedHashMap<>();
    _nameToJava = new LinkedHashMap<>();
    ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
    try( Connection c = cp.getConnection( dbConfig ) )
    {
      _driverInfo = DriverInfo.lookup( c.getMetaData() );

      DatabaseMetaData metaData = c.getMetaData();

      String schemaName = findSchemaNameAsSchema( metaData );
      String catalogName = null;
      if( schemaName == null )
      {
        // mysql, being mysql, provides the schema as a catalog
        catalogName = findSchemaNameAsCatalog( metaData );
      }
      _schemaIsCatalog = catalogName != null;
      String name = _schemaIsCatalog ? catalogName : schemaName;
      if( name == null && (_hasSchemas || _hasCatalogs) )
      {
        throw new SQLException( "None of: '" + dbConfig.getName() +
          "' dbconfig file name, \"schemaName\", or \"catalogName\" match a schema name in " +
          metaData.getDatabaseProductName() + "." );
      }
      if( name != null )
      {
        if( _driverInfo == Oracle || metaData.storesUpperCaseIdentifiers() )
        {
          name = name.toUpperCase();
        }
        else if( metaData.storesLowerCaseIdentifiers() )
        {
          name = name.toLowerCase();
        }
      }
      _name = name;

      build( c, metaData );
    }
    catch( SQLException se )
    {
      throw se;
    }
    catch( Exception e )
    {
      LOGGER.warn( "Suspicious exception.", e );
      throw new SQLException( e );
    }
  }

  private void build( Connection c, DatabaseMetaData metaData ) throws SQLException
  {
    String catalog = _schemaIsCatalog ? _name : _dbConfig.getCatalogName();
    String schema = _schemaIsCatalog ? null : _name;
    LOGGER.info( "JdbcSchema building: catalog: " + catalog + ", schema: " + schema );
    assignTableNames( catalog, schema, metaData ); // necessary to assign table names before creating tables due to forward refs
    try( ResultSet resultSet = metaData.getTables( catalog, schema, getTableNamePattern(), getTableTableTypes( metaData ) ) )
    {
      while( resultSet.next() )
      {
        JdbcSchemaTable table = new JdbcSchemaTable( this, metaData, resultSet );
        String name = table.getName();
        _tables.put( name, table );
      }
    }
    catch( SQLException e )
    {
      // this is dicey, but some drivers (looking at you duckdb) appear to fail after successfully iterating this resultset
      LOGGER.warn( "JdbcSchema: build may not have completed.", e );
    }

    for( SchemaTable table : _tables.values() )
    {
      table.resolveForeignKeys();
    }

    for( SchemaTable table : _tables.values() )
    {
      table.resolveFkRelations();
    }
  }

  private void assignTableNames( String catalog, String schema, DatabaseMetaData metaData )
  {
    Map<String, List<TableData>> tables = new LinkedHashMap<>();
    try( ResultSet resultSet = metaData.getTables( catalog, schema, getTableNamePattern(), getTableTableTypes( metaData ) ) )
    {
      while( resultSet.next() )
      {
        String name = resultSet.getString( "TABLE_NAME" );
        String type = resultSet.getString( "TABLE_TYPE" );
        String javaName = makePascalCaseIdentifier( name, true );
        tables.computeIfAbsent( javaName, __ -> new ArrayList<>() )
          .add( new TableData( name, type ) );
      }
    }
    catch( SQLException e )
    {
      // this is dicey, but some drivers (looking at you duckdb) appear to fail after successfully iterating this resultset
      LOGGER.warn( "JdbcSchema: build may not have completed.", e );
    }

    for( Map.Entry<String, List<TableData>> entry: tables.entrySet() )
    {
      String javaName = entry.getKey();
      List<TableData> list = entry.getValue();
      if( list.size() == 1 )
      {
        // only one table mapped to the java name, assign the name as usual

        TableData tableData = list.get( 0 );
        _javaToName.put( javaName, tableData.rawName );
        _nameToJava.put( tableData.rawName, javaName );
      }
      else if( list.size() == 2 && !list.get( 0 ).type.equals( list.get( 1 ).type ) )
      {
        // for case where a table and a view resolve to the same java name,
        // reserve the java name for the table and make a new one for the view

        TableData first = list.get( 0 );
        TableData second = list.get( 1 );
        TableData table = null;
        TableData view = null;
        if( first.type.toLowerCase().contains( "view" ) )
        {
          table = second;
          view = first;
        }
        else if( second.type.toLowerCase().contains( "view" ) )
        {
          table = first;
          view = second;
        }

        if( view != null )
        {
          _javaToName.put( javaName, table.rawName );
          _nameToJava.put( table.rawName, javaName );

          javaName += "_view";
          _javaToName.put( javaName, view.rawName );
          _nameToJava.put( view.rawName, javaName );
        }
        else
        {
          simpleNameChange( list, javaName );
        }
      }
      else
      {
        // more than two tables/views resolve to the same java name, just append _1 etc. to the java names
        simpleNameChange( list, javaName );
      }
    }
  }

  private void simpleNameChange( List<TableData> list, String javaName )
  {
    for( int i = 0; i < list.size(); i++ )
    {
      TableData tableData = list.get( i );
      javaName += "_" + (i+1);
      _javaToName.put( javaName, tableData.rawName );
      _nameToJava.put( tableData.rawName, javaName );
    }
  }

  private static class TableData
  {
    private final String rawName;
    private final String type;

    TableData( String name, String type )
    {
      this.rawName = name;
      this.type = type;
    }
  }

  private String getTableNamePattern()
  {
    return isDb2() ? "%" : null;
  }

  private boolean isDb2()
  {
    return _driverInfo.getDriversInUse().keySet().stream().anyMatch( k -> k.startsWith( "IBM " ) );
  }

  @NotNull
  private static String[] getTableTableTypes( DatabaseMetaData metaData ) throws SQLException
  {
    List<String> tableTypes = new ArrayList<>();
    List<String> allTypes = new ArrayList<>();
    try( ResultSet rs = metaData.getTableTypes() )
    {
      while( rs.next() )
      {
        String tableType = rs.getString( "TABLE_TYPE" );
        allTypes.add( tableType );
        if( TABLE_TYPES.contains( tableType ) )
        {
          tableTypes.add( tableType );
        }
      }
    }
    if( tableTypes.isEmpty() )
    {
      throw new SQLException( "No valid table types found from '" +
        metaData.getDatabaseProductName() + "': " + allTypes );
    }
    return tableTypes.toArray( new String[0] );
  }

  private String findSchemaNameAsSchema( DatabaseMetaData metaData ) throws SQLException
  {
    String catalogName = _dbConfig.getCatalogName();
    String schemaName = _dbConfig.getSchemaName();
    String publicName = null;
    String rawNameMatch = null;
    try( ResultSet schemas = catalogName != null
      ? metaData.getSchemas( catalogName, schemaName ) // sqlite throws SQLFeatureNotSupportedException for this one
      : metaData.getSchemas() )
    {
      while( schemas.next() )
      {
        _hasSchemas = true;

        String rawName = schemas.getString( "TABLE_SCHEM" );

        if( rawName.equalsIgnoreCase( schemaName ) )
        {
          // matches "schemaName" property, this has precedence over dbconfig file name match
          rawNameMatch = rawName;
          break;
        }

        if( matchesDbConfigFileName( rawName, schemaName ) )
        {
          rawNameMatch = rawName;
          // continue trying to match against "schemaName" property
        }

        if( rawName.equalsIgnoreCase( "public" ) )
        {
          // if no match is found and a 'public' schema exists, use that (with a warning)
          publicName = rawName;
        }
      }
    }
    if( rawNameMatch != null )
    {
      return rawNameMatch;
    }
    if( publicName != null )
    {
      LOGGER.warn( "No schema found in database '" + metaData.getDatabaseProductName() +
        "' that matches dbconfig file name '" + _dbConfig.getName() + "' or 'schemaName', defaulting to 'public' schema." );
      return publicName;
    }
    return null;
  }

  private String findSchemaNameAsCatalog( DatabaseMetaData metaData ) throws SQLException
  {
    String catalogName = _dbConfig.getCatalogName();
    catalogName = catalogName == null ? _dbConfig.getSchemaName() : catalogName;
    String rawNameMatch = null;
    try( ResultSet catalogs =  metaData.getCatalogs() )
    {
      while( catalogs.next() )
      {
        _hasCatalogs = true;

        String rawName = catalogs.getString( "TABLE_CAT" );

        if( rawName.equalsIgnoreCase( catalogName ) )
        {
          // matches "catalogName" property, this has precedence over dbconfig file name match
          rawNameMatch = rawName;
          break;
        }

        if( matchesDbConfigFileName( rawName, catalogName ) )
        {
          rawNameMatch = rawName;
          // continue trying to match against "catalogName" property
        }
      }
    }
    return rawNameMatch;
  }

  private boolean matchesDbConfigFileName( String rawName, String schemaName )
  {
    String dbConfigFileName = getDbConfig().getName();
    if( rawName.equalsIgnoreCase( dbConfigFileName ) ||
      ManIdentifierUtil.makePascalCaseIdentifier( rawName, true ).equalsIgnoreCase( dbConfigFileName ) )
    {
      // matches dbconfig file name
      return true;
    }
    return false;
  }

  @Override
  public boolean isCatalogBased()
  {
    return _schemaIsCatalog;
  }

  @Override
  public String getCatalog()
  {
    return getDbConfig().getCatalogName();
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public DbConfig getDbConfig()
  {
    return _dbConfig;
  }

  @Override
  public boolean hasTable( String name )
  {
    return _nameToJava.containsKey( name ) || _javaToName.containsKey( name );
  }

  @Override
  public SchemaTable getTable( String name )
  {
    SchemaTable table = _tables.get( name );
    if( table == null )
    {
      table = _tables.get( getOriginalName( name ) );
    }
    return table;
  }

  @Override
  public Map<String, SchemaTable> getTables()
  {
    return _tables;
  }

  @Override
  public String getJavaTypeName( String name )
  {
    return _nameToJava.get( name );
  }

  @Override
  public String getOriginalName( String javaName )
  {
    return _javaToName.get( javaName );
  }

  @Override
  public DriverInfo getDriverInfo()
  {
    return _driverInfo;
  }
}
