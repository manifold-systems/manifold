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

import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.DbConfigProvider;
import manifold.sql.rt.api.ExecutionEnv;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * loads DbConfig from .dbconfig file
 */
public class DbConfigFinder implements DbConfigProvider
{
  public static final String DBCONFIG_EXT = ".dbconfig";

  private final Map<String, DbConfig> _configs;

  public DbConfigFinder()
  {
    _configs = new LinkedHashMap<>();
  }

  public DbConfig loadDbConfig( String configName, Class<?> ctx )
  {
    return _configs.computeIfAbsent( configName, __ -> findConfig( configName, ctx ) );
  }

  @Override
  public void clear()
  {
    _configs.clear();
  }

  private DbConfig findConfig( String configName, Class<?> ctx )
  {
    InputStream stream = findConfigInCurrentDir( configName );
    if( stream == null && ctx != null )
    {
      stream = findConfigAsResource( configName, ctx );
    }
    if( stream == null )
    {
      return null;
    }

    try( Reader reader = new InputStreamReader( stream ) )
    {
      Bindings bindings = (Bindings)Json.fromJson( StreamUtil.getContent( reader ) );
      bindings.put( "name", configName );
      return new DbConfigImpl( null, bindings, ExecutionEnv.Runtime );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private InputStream findConfigInCurrentDir( String configName )
  {
    // look for file in /config of current dir, failing that look in current dir
    File file = new File( "./config/" + configName + DBCONFIG_EXT );
    if( !file.isFile() )
    {
      file = new File( "./" + configName + DBCONFIG_EXT );
      if( !file.isFile() )
      {
        return null;
      }
    }
    try
    {
      return Files.newInputStream( file.toPath() );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private InputStream findConfigAsResource( String configName, Class<?> ctx )
  {
    // look for resource file in /<module-name>/config, or just /config if no module
    String module = getModuleName( ctx );
    InputStream stream = ctx.getResourceAsStream(
      '/' + (module == null || module.isEmpty() ? "config/" : module + "/config/") + configName + DBCONFIG_EXT );
    if( stream == null )
    {
      if( module != null && !module.isEmpty() )
      {
        // look in just /config
        stream = ctx.getResourceAsStream(  "/config/" + configName + DBCONFIG_EXT );
      }
    }
    return stream;
  }

  private String getModuleName( Class<?> ctx )
  {
    Object module = JreUtil.isJava8() || ctx == null
      ? null
      : ReflectUtil.method( (Object)ctx, "getModule" ).invoke();
    String moduleName = module != null && (boolean)ReflectUtil.method( module, "isNamed" ).invoke()
      ? (String)ReflectUtil.method( module, "getName" ).invoke()
      : null;
    moduleName = moduleName == null ? null : moduleName.replace( '.', '_' );
    return moduleName;
  }

}