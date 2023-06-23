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

import manifold.rt.api.Bindings;
import manifold.sql.rt.api.TypeMap;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.schema.api.Schema;
import manifold.sql.rt.connection.TestContextProvider;
import manifold.util.ManExceptionUtil;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class JdbcSchema implements Schema
{
  private final String _name;

  private final Map<String, JdbcSchemaTable> _tables;
  private final TypeMap _typeMap;

  public JdbcSchema( DbConfig dbConfig )
  {
    _name = dbConfig.getName();
    _tables = new LinkedHashMap<>();
    _typeMap = TypeMap.findFirst();
    loadDriverClass( dbConfig );
    Properties props = dbConfig.toProperties();
    try( Connection c = DriverManager.getConnection( dbConfig.getBuildUrlOtherwiseRuntimeUrl(), props ) )
    {
      build( c );
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private void build( Connection c ) throws SQLException
  {
    for( TestContextProvider p : TestContextProvider.PROVIDERS.get() )
    {
      p.init( c );
    }

    DatabaseMetaData metaData = c.getMetaData();
    try( ResultSet resultSet = metaData.getTables( null, null, null, new String[]{"TABLE", "VIEW"} ) )
    {
      while( resultSet.next() )
      {
        JdbcSchemaTable table = new JdbcSchemaTable( c, this, metaData, resultSet );
        _tables.put( table.getName(), table );
      }
    }

    for( JdbcSchemaTable table : _tables.values() )
    {
      table.resolve();
    }
  }

  private void loadDriverClass( DbConfig dbConfig )
  {
    try
    {
      Class.forName( dbConfig.getDriverClass() );
    }
    catch( ClassNotFoundException e )
    {
      throw new RuntimeException( e );
    }
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public boolean hasTable( String name )
  {
    return _tables.containsKey( name );
  }

  @Override
  public JdbcSchemaTable getTable( String name )
  {
    return _tables.get( name );
  }

  @Override
  public Map<String, JdbcSchemaTable> getTables()
  {
    return _tables;
  }

  public TypeMap getTypeMap()
  {
    return _typeMap;
  }
}
