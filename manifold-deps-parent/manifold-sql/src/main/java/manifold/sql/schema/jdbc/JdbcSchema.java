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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;
import static manifold.sql.rt.util.DriverInfo.Oracle;

public class JdbcSchema implements Schema
{
  private static final Logger LOGGER = LoggerFactory.getLogger( JdbcSchema.class );

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
    try( ResultSet resultSet = metaData.getTables( catalog, schema, null, new String[]{"TABLE", "VIEW"} ) )
    {
      while( resultSet.next() )
      {
        JdbcSchemaTable table = new JdbcSchemaTable( this, metaData, resultSet );
        String name = table.getName();
        _tables.put( name, table );
        String javaName = makePascalCaseIdentifier( name, true );
        _javaToName.put( javaName, name );
        _nameToJava.put( name, javaName );
      }
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
    return _tables.containsKey( name ) || _tables.containsKey( getOriginalName( name ) );
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
