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

package manifold.sql.rt.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import manifold.rt.api.Bindings;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.util.ManExceptionUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HikariConnectionProvider implements ConnectionProvider
{
  private Map<String, HikariDataSource> _dataSources = new LinkedHashMap<>();

  @Override
  public Connection getConnection( String configName, Class<?> classContext )
  {
    HikariDataSource ds = _dataSources.computeIfAbsent( configName, __ -> {
      DbConfig dbConfig = DbConfigFinder.instance().findConfig( configName, classContext );
      if( dbConfig == null )
      {
        throw new RuntimeException( "Could not find a .dbconfig file for \"" + configName + "\"" );
      }

      loadDriverClass( dbConfig );

      HikariConfig config = new HikariConfig( dbConfig.toProperties() );
      config.setJdbcUrl( dbConfig.getUrl() );
      return new HikariDataSource( config );
    } );
    try
    {
      return ds.getConnection();
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
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
}
