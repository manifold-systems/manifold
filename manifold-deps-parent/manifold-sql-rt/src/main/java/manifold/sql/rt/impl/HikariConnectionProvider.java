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

package manifold.sql.rt.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import manifold.rt.api.util.ServiceUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;
import manifold.util.ManExceptionUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HikariConnectionProvider implements ConnectionProvider
{
  private final Map<String, HikariDataSource> _dataSources = new ConcurrentHashMap<>();

  public HikariConnectionProvider()
  {
    // ensure jdbc drivers are loaded
    Set<Driver> drivers = new HashSet<>();
    ServiceUtil.loadRegisteredServices( drivers, Driver.class, ConnectionProvider.class.getClassLoader() );
  }

  @Override
  public Connection getConnection( String configName, Class<?> classContext )
  {
    //noinspection resource
    HikariDataSource ds = _dataSources.computeIfAbsent( configName, __ -> {
      DbConfig dbConfig = Dependencies.instance().getDbConfigProvider().loadDbConfig( configName, classContext );
      if( dbConfig == null )
      {
        throw ManExceptionUtil.unchecked(
          new SQLException( "Could not find DbConfig for \"" + configName + "\", " +
            "class context: " + classContext.getTypeName() ) );
      }

      return makeDataSource( dbConfig, dbConfig.getUrl() );
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

  @Override
  public Connection getConnection( DbConfig dbConfig ) throws SQLException
  {
    //noinspection resource
    HikariDataSource ds = _dataSources.computeIfAbsent( dbConfig.getName(), __ ->
      makeDataSource( dbConfig, dbConfig.getBuildUrlOtherwiseRuntimeUrl() ) );
    return ds.getConnection();
  }

  @Override
  public void closeDataSource( DbConfig dbConfig )
  {
    HikariDataSource dropped = _dataSources.remove( dbConfig.getName() );
    if( dropped != null )
    {
      dropped.close();
    }
  }

  private HikariDataSource makeDataSource( DbConfig dbConfig, String url )
  {
    HikariConfig config = new HikariConfig( dbConfig.toProperties() );
    config.setJdbcUrl( url );
    return new HikariDataSource( config );
  }

  @Override
  public void closeAll()
  {
    _dataSources.values().forEach( ds -> ds.close() );
    _dataSources.clear();
  }
}
