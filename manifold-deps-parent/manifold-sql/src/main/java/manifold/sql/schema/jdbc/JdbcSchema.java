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

import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.schema.api.Schema;
import manifold.sql.rt.api.ConnectionNotifier;
import manifold.sql.schema.api.SchemaTable;
import manifold.util.ManExceptionUtil;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static manifold.rt.api.util.ManIdentifierUtil.makePascalCaseIdentifier;

public class JdbcSchema implements Schema
{
  private final String _name;
  private final DbConfig _dbConfig;
  private final Map<String, SchemaTable> _tables;
  private final Map<String, String> _javaToName;
  private final Map<String, String> _nameToJava;
  private final String _dbProductName;
  private final String _dbProductVersion;

  public JdbcSchema( DbConfig dbConfig )
  {
    _dbConfig = dbConfig;
    _tables = new LinkedHashMap<>();
    _javaToName = new LinkedHashMap<>();
    _nameToJava = new LinkedHashMap<>();
    ConnectionProvider cp = ConnectionProvider.findFirst();
    try( Connection c = cp.getConnection( dbConfig ) )
    {
      _dbProductName = c.getMetaData().getDatabaseProductName();
      _dbProductVersion = c.getMetaData().getDatabaseProductVersion();

      DatabaseMetaData metaData = c.getMetaData();
      _name = findSchemaName( metaData );

      build( c, metaData );
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private void build( Connection c, DatabaseMetaData metaData ) throws SQLException
  {
    for( ConnectionNotifier p : ConnectionNotifier.PROVIDERS.get() )
    {
      p.init( c );
    }

     try( ResultSet resultSet = metaData.getTables(
      _dbConfig.getCatalogName(), _name, null, new String[]{"TABLE", "VIEW"} ) )
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
      table.resolve();
    }
  }

  String findSchemaName( DatabaseMetaData metaData ) throws SQLException
  {
    String defaultSchema = null;
    String catalogName = _dbConfig.getCatalogName();
    String schemaName = _dbConfig.getSchemaName();
    try( ResultSet schemas = catalogName != null
      ? metaData.getSchemas( catalogName, schemaName ) // sqlite throws SQLFeatureNotSupportedException for this one
      : metaData.getSchemas() )
    {
      while( schemas.next() )
      {
        String schem = schemas.getString( "TABLE_SCHEM" );

        if( schem.equalsIgnoreCase( schemaName ) )
        {
          return schem;
        }

        if( schem.equalsIgnoreCase( getDbConfig().getName() ) )
        {
          return schem;
        }

        if( !schem.equalsIgnoreCase( "information_schema" ) )
        {
          defaultSchema = schem;
        }
      }
    }
    return defaultSchema;
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

  public String getJavaTypeName( String name )
  {
    return _nameToJava.get( name );
  }

  public String getOriginalName( String javaName )
  {
    return _javaToName.get( javaName );
  }

  @Override
  public String getDatabaseProductName()
  {
    return _dbProductName;
  }

  @Override
  public String getDatabaseProductVersion()
  {
    return _dbProductVersion;
  }
}
