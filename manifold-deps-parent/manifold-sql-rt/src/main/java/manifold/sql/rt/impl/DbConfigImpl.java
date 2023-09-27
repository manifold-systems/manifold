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
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.DbLocationProvider.Mode;
import manifold.sql.rt.util.PropertyExpressionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static manifold.sql.rt.api.DbLocationProvider.Mode.Unknown;

public class DbConfigImpl implements DbConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger( DbConfigImpl.class );

  public static final DbConfig EMPTY = new DbConfigImpl( null, DataBindings.EMPTY_BINDINGS, Unknown );

  private final Bindings _bindings;
  private final Map<String, List<Consumer<Connection>>> _initializers;

  public DbConfigImpl( Function<String, FqnCache<IFile>> resByExt, Bindings bindings, Mode mode )
  {
    this( resByExt, bindings, mode, null );
  }

  /**
   * Type-safe access to configuration from .dbconfig files.
   *
   * @param bindings JSON bindings from a .dbconfig file
   * @param exprHandler An optional handler to evaluate expressions in URL fields
   */
  public DbConfigImpl( Function<String, FqnCache<IFile>> resByExt, Bindings bindings, Mode mode, Function<String, String> exprHandler )
  {
    _initializers = new HashMap<>();
    processUrl( resByExt, bindings, mode, "url", exprHandler );
    processUrl( resByExt, bindings, mode, "buildUrl", exprHandler );
    _bindings = bindings;
    assignDefaults();
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

  private void processUrl( Function<String, FqnCache<IFile>> resByExt, Bindings bindings, Mode mode, String key, Function<String, String> exprHandler )
  {
    String url = (String)bindings.get( key );
    if( url == null )
    {
      return;
    }
    PropertyExpressionProcessor.Result result = PropertyExpressionProcessor.process( resByExt, url, mode, exprHandler );
    bindings.put( key, result.url );
    _initializers.put( result.url, result.initializers ); // for testing purposes
  }

  @Override
  public void init( Connection connection, String url )
  {
    // this is for testing purposes e.g., dynamically creating a db from a ddl script before the db is accessed

    List<Consumer<Connection>> consumers = _initializers.get( url );
    for( Consumer<Connection> consumer : consumers )
    {
      consumer.accept( connection );
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
  public String getPassword()
  {
    return (String)_bindings.get( "password" );
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
