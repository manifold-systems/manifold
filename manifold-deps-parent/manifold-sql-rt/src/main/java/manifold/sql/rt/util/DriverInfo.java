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

package manifold.sql.rt.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public enum DriverInfo
{
  H2( "H2", false, false ),
  MySQL( "MySQL", false, true ),
  Oracle( "Oracle", true, false ),
  Postgres( "PostgreSQL", false, false ),
  SQLite( "SQLite", false, true ),
  SqlServer( "SQL Server", true, false ),
  UNTESTED( null, false, false ),
  ERRANT( null, false, false );

  public static final Logger LOGGER = LoggerFactory.getLogger( DriverInfo.class );

  private final String _productName;
  private final boolean _requiresQueryExecForTableName;
  private final boolean _flakyParameterMetadata;
  private final Map<String, Set<String>> _driversInUse;

  DriverInfo( String productName, boolean requiresQueryExecForTableName, boolean flakyParameterMetadata )
  {
    _productName = productName;
    _requiresQueryExecForTableName = requiresQueryExecForTableName;
    _flakyParameterMetadata = flakyParameterMetadata;
    _driversInUse = new LinkedHashMap<>();
  }

  public String getDriverBaseName()
  {
    return _productName;
  }

  /**
   * For logging/testing purposes, helps to know the full driver name. Typically, there is only one, unless multiple
   * dbconfigs are in use with multiple/untested drivers.
   */
  public Map<String, Set<String>> getDriversInUse()
  {
    return _driversInUse;
  }
  private void addDriver( DatabaseMetaData metadata ) throws SQLException
  {
    String name = metadata.getDriverName();
    String version = metadata.getDriverVersion();
    if( _driversInUse.computeIfAbsent( name, __ -> new LinkedHashSet<>() )
          .add( version ) )
    {
      LOGGER.info( "Driver in use: '" + name + "' version: " + version +
        " DB: " + metadata.getDatabaseProductName() + " DB version: " + metadata.getDatabaseProductVersion() );
    }
  }

  /**
   * Some drivers' query column metadata does not provide the table name info if the query is not first executed before
   * metadata collected. As a consequence, we must execute the query before we get metadata (ouch) or use an off-the-shelf
   * parser to infer the table name (ouch).
   * <p/>
   * Perpetrators: {@link #Oracle}, {@link #SqlServer}
   */
  public boolean requiresQueryExecForTableName()
  {
    return _requiresQueryExecForTableName;
  }

  /**
   * (circus music)
   * Some drivers don't provide query parameter types when the parameter's value is not set O_o For instance, depending
   * on the version, SQLite will either return VARCHAR for all parameters or it will throw an exception when a parameter
   * is not set. Similarly, Mysql throws the exception too unless the "generateSimpleParameterMetadata" url parameter is
   * set in which case it provides the same VARCHAR crap sandwich as sqlite (as tested with mysql driver 8.1).
   * <p/>
   * Perpetrators: {@link #SQLite}, {@link #MySQL}
   */
  public boolean flakyParameterMetadata()
  {
    return _flakyParameterMetadata;
  }

  public static DriverInfo lookup( DatabaseMetaData metadata ) throws SQLException
  {
    DriverInfo driver = lookup( metadata.getDriverName() );
    if( driver != ERRANT )
    {
      driver.addDriver( metadata );
    }
    return driver;
  }

  private static DriverInfo lookup( String name )
  {
    if( name == null )
    {
      // errant dbconfig
      return ERRANT;
    }

    DriverInfo result;
    if( name.contains( H2.getDriverBaseName() ) )
    {
      result = H2;
    }
    else if( name.contains( MySQL.getDriverBaseName() ) )
    {
      result = MySQL;
    }
    else if( name.contains( Oracle.getDriverBaseName() ) )
    {
      result = Oracle;
    }
    else if( name.contains( Postgres.getDriverBaseName() ) )
    {
      result = Postgres;
    }
    else if( name.contains( SQLite.getDriverBaseName() ) )
    {
      result = SQLite;
    }
    else if( name.contains( SqlServer.getDriverBaseName() ) )
    {
      result = SqlServer;
    }
    else
    {
      result = UNTESTED;
    }
    return result;
  }
}
