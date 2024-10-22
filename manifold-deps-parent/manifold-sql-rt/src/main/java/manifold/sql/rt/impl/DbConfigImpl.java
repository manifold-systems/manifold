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

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;
import manifold.json.rt.api.DataBindings;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.ExecutionEnv;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.rt.util.PropertyExpressionProcessor;
import manifold.sql.rt.util.SqlScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static manifold.sql.rt.api.ExecutionEnv.*;
import static manifold.sql.rt.util.DriverInfo.Oracle;

public class DbConfigImpl implements DbConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger( DbConfigImpl.class );

  public static final DbConfig EMPTY = new DbConfigImpl( null, DataBindings.EMPTY_BINDINGS, Unknown );
  private static final Set<String> DDL = new LinkedHashSet<>();

  private final Bindings _bindings;
  private final Map<String, List<Consumer<Connection>>> _initializers;
  private final transient Function<String, FqnCache<IFile>> _resByExt;
  private final ExecutionEnv _env;

  public DbConfigImpl( Function<String, FqnCache<IFile>> resByExt, Bindings bindings, ExecutionEnv executionEnv )
  {
    this( resByExt, bindings, executionEnv, null );
  }

  /**
   * Type-safe access to configuration from .dbconfig files.
   *
   * @param bindings JSON bindings from a .dbconfig file
   * @param exprHandler An optional handler to evaluate expressions in URL fields
   */
  public DbConfigImpl( Function<String, FqnCache<IFile>> resByExt, Bindings bindings, ExecutionEnv executionEnv, Function<String, String> exprHandler )
  {
    _initializers = new HashMap<>();
    _env = executionEnv;
    processUrl( resByExt, bindings, executionEnv, "url", exprHandler );
    processUrl( resByExt, bindings, executionEnv, "buildUrl", exprHandler );
    _bindings = bindings;
    _resByExt = resByExt;
    assignDefaults();
  }

  /** For testing only!! */
  public DbConfigImpl( Bindings bindings, ExecutionEnv executionEnv )
  {
    this( null, bindings, executionEnv );
  }

  private void assignDefaults()
  {
    if( _bindings.isEmpty() )
    {
      // empty bindings indicates EMPTY bindings, such as DataBindings.EMPTY_BINDINGS,
      // which are immutable
      return;
    }

    String schemaPackage = getSchemaPackage();
    if( schemaPackage == null || schemaPackage.isEmpty() )
    {
      _bindings.put( "schemaPackage", DbConfig.DEFAULT_SCHEMA_PKG );
      LOGGER.info( "No 'schemaPackage' defined in DbConfig: '" + getName() + "'. Using default: '" + schemaPackage + "'." );
    }
  }

  private void processUrl( Function<String, FqnCache<IFile>> resByExt, Bindings bindings, ExecutionEnv executionEnv, String key, Function<String, String> exprHandler )
  {
    String url = (String)bindings.get( key );
    if( url == null )
    {
      return;
    }
    PropertyExpressionProcessor.Result result = PropertyExpressionProcessor.process( resByExt, url, executionEnv, exprHandler );
    bindings.put( key, result.url );
    _initializers.put( result.url, result.initializers ); // for testing purposes
  }

  @Override
  public ExecutionEnv getEnv()
  {
    return _env;
  }

  @Override
  public void init( Connection connection, ExecutionEnv env ) throws SQLException
  {
    if( _env != env )
    {
      throw new RuntimeException( "envs disagree: " + "_env: " + _env + "  env: " + env );
    }

    List<Consumer<Connection>> consumers =
      _initializers.get( env == Compiler || env == IDE ? getBuildUrlOtherwiseRuntimeUrl() : getUrl() );
    for( Consumer<Connection> consumer : consumers )
    {
      // this is for testing purposes e.g., dynamically creating a db from a ddl script before the db is accessed
      consumer.accept( connection );
    }

    syncWithConnection( connection );

    if( env == Compiler || env == IDE )
    {
      // for testing, only relevant during compilation: need to create db from ddl
      // Note, test execution framework handles DDL loading explicitly, again this is only relevant during compilation for testing
      execDdl( connection, getDbDdl() );
    }
  }

  /**
   * If this dbconfig provides a catalogName and/or a schemaName, assign them _to_ the connection.
   * Otherwise, if provided from the JDBC URL, assign dbconfig's catalog and/or schema _from_ the connection.
   */
  private void syncWithConnection( Connection connection ) throws SQLException
  {
    String catalogName = getCatalogName();
    if( catalogName != null )
    {
      // set the connection's catalog
      connection.setCatalog( catalogName );
    }
    else
    {
      String catalog = connection.getCatalog();
      if( catalog != null )
      {
        // assign dbconfig catalogName from connection
        _bindings.put( "catalogName", catalog );
      }
    }

    String schemaName = getSchemaName();
    if( schemaName != null && !schemaName.isEmpty() )
    {
      // set the connection's schema
      connection.setSchema( schemaName );
    }
    else
    {
      String schema = connection.getSchema();
      if( schema != null )
      {
        // assign dbconfig schemaName from connection
        _bindings.put( "schemaName", schema );
      }
    }
  }

  // for testing
  private void execDdl( Connection connection, String ddl ) throws SQLException
  {
    if( ddl == null || ddl.isEmpty() || (!isInMemory() && DDL.contains( ddl )) )
    {
      return;
    }
    DDL.add( ddl );

    if( !ddl.startsWith( "/" ) && !ddl.startsWith( "\\" ) )
    {
      ddl = "/" + ddl;
    }

    IFile ddlFile = null;
    if( _resByExt != null )
    {
      // at compile-time we must find the ddl resource file
      ddlFile = ResourceDbLocationProvider.maybeGetCompileTimeResource( _resByExt, ExecutionEnv.Compiler, ddl );
      //_resByExt = null;
    }


    try( InputStream stream = ddlFile == null ? getClass().getResourceAsStream( ddl ) : ddlFile.openInputStream() )
    {
      if( stream == null )
      {
        throw new RuntimeException( "No resource file found matching: " + ddl );
      }

      String script = StreamUtil.getContent( new InputStreamReader( stream ) );
      DriverInfo driver = DriverInfo.lookup( connection.getMetaData() );
      SqlScriptRunner.runScript( connection, script,
        driver == Oracle  // let drop user fail and continue running the script (hard otherwise with oracle :\)
        ? (s, e) -> s.toLowerCase().contains( "drop user " )
        : null );
    }
    catch( Exception e )
    {
      if( isInMemory() && e.getMessage().contains( "already exists" ) )
      {
        // in-memory test databases (h2, sqlite) are hard to get idempotent ddl,
        // so we assume "already exits" means the ddl is already there, good enough for tests :\
        return;
      }
      throw new SQLException( e );
    }
  }

  @Override
  public String getName()
  {
    return (String)_bindings.get( "name" );
  }

  @Override
  public String getCatalogName()
  {
    return (String)_bindings.get( "catalogName" );
  }

  @Override
  public String getSchemaName()
  {
    return (String)_bindings.get( "schemaName" );
  }

  @Override
  public String getPath()
  {
    return (String)_bindings.get( "path" );
  }

  @Override
  public String getUrl()
  {
    return (String)_bindings.get( "url" );
  }

  @Override
  public String getBuildUrl()
  {
    return (String)_bindings.get( "buildUrl" );
  }

  @Override
  public String getUser()
  {
    return (String)_bindings.get( "user" );
  }

  @Override
  public String getBuildUser()
  {
    return (String)_bindings.get( "buildUser" );
  }

  @Override
  public String getPassword()
  {
    return (String)_bindings.get( "password" );
  }

  @Override
  public String getBuildPassword()
  {
    return (String)_bindings.get( "buildPassword" );
  }

  @Override
  public boolean isDefault()
  {
    Boolean isDefault = (Boolean)_bindings.get( "isDefault" );
    return isDefault != null && isDefault;
  }

  @Override
  public String getSchemaPackage()
  {
    return (String)_bindings.get( "schemaPackage" );
  }

  @Override
  public Bindings getProperties()
  {
    return (Bindings)_bindings.get( "properties" );
  }

  @Override
  public String getDbDdl()
  {
    return (String)_bindings.get( "dbDdl" );
  }

  @Override
  public String getCustomBaseInterface()
  {
    return (String)_bindings.get( "customBaseInterface" );
  }
  @Override
  public String getCustomBaseClass()
  {
    return (String)_bindings.get( "customBaseClass" );
  }

  @Override
  public boolean isInMemory()
  {
    Boolean inMemory = (Boolean)_bindings.get( "inMemory" );
    return inMemory != null && inMemory;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o ) return true;
    if( !(o instanceof DbConfigImpl) ) return false;
    DbConfigImpl dbConfig = (DbConfigImpl)o;
    return _bindings.equals( dbConfig._bindings );
  }

  @Override
  public int hashCode()
  {
    return _bindings.hashCode();
  }
}
