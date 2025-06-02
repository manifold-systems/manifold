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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static manifold.sql.rt.api.ExecutionEnv.*;

public class HikariConnectionProvider implements ConnectionProvider
{
  private final Map<String, HikariDataSource> _dataSources = new ConcurrentHashMap<>();

  public HikariConnectionProvider()
  {
    // ensure jdbc drivers are loaded
    Set<Driver> drivers = new HashSet<>();
    ServiceUtil.loadRegisteredServices( drivers, Driver.class, ConnectionProvider.class.getClassLoader() );
  }

  /** This method is exclusive to <b>runtime</b> use, as opposed to compile-time. */
  @Override
  public Connection getConnection( String configName, Class<?> classContext )
  {
    DbConfig[] dbConfig = {null};
    HikariDataSource ds = _dataSources.computeIfAbsent( configName, __ -> {
      dbConfig[0] = Dependencies.instance().getDbConfigProvider().loadDbConfig( configName, classContext );
      if( dbConfig[0] == null )
      {
        throw ManExceptionUtil.unchecked(
          new SQLException( "Could not find DbConfig for \"" + configName + "\", " +
            "class context: " + classContext.getTypeName() ) );
      }
      return makeDataSource( dbConfig[0], dbConfig[0].getUrl() );
    } );
    try
    {
      Connection connection = ds.getConnection();
      try
      {
        if( dbConfig[0] != null )
        {
          dbConfig[0].init( connection, Runtime );
        }
      }
      catch( Throwable t )
      {
        connection.close();
        throw t;
      }
      return connection;
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  /** This method is exclusive to <b>Compile/IDE-time</b> use. It is used exclusively to read metadata from the DB.*/
  @Override
  public Connection getConnection( DbConfig dbConfig ) throws SQLException
  {
    Connection connection;
    if( dbConfig.isFileBased() )
    {
      // File-based DBs maintain connections with a file lock, even when idle, so pooling must be avoided.
      // Basically, the lock prevents another process from opening a connection e.g., trying to build in IJ:
      // IDE process has idle connection pooled with file open/locked, and build process will fail to open connection.
      // To address this we avoid pooling the connection while in IDE/build-time with NonPooledConnection.
      //
      // Note, *some* file-based DBs will avoid locking the file if the connection is in read-only mode. Thus,
      // given this connection is used exclusively for reading metadata from the DB, we *could* set it to read-only
      // mode. However, Hikari won't/can't create a new pool based on a different read-only state for a URL having already
      // established a pool -- PoolInitializationException results otherwise. This behavior is observable with SQLite
      // file-based URLs.
      HikariDataSource ds = makeDataSource( dbConfig, dbConfig.getBuildUrlOtherwiseRuntimeUrl() );
      connection = new NonPooledConnection( ds, ds.getConnection() );
    }
    else
    {
      HikariDataSource ds = _dataSources.computeIfAbsent( dbConfig.getName(), __ ->
        makeDataSource( dbConfig, dbConfig.getBuildUrlOtherwiseRuntimeUrl() ) );
      connection = ds.getConnection();
    }

    try
    {
      dbConfig.init( connection, Compiler );
    }
    catch( Throwable t )
    {
      connection.close();
      throw t;
    }
    return connection;
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
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl( url );
    config.setDataSourceProperties( dbConfig.toProperties() );
    return new HikariDataSource( config );
  }

  @Override
  public void closeAll()
  {
    _dataSources.values().forEach( ds -> ds.close() );
    _dataSources.clear();
  }
}
