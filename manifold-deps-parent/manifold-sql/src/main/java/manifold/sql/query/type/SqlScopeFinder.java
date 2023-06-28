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

package manifold.sql.query.type;

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;
import manifold.internal.javac.IIssue;
import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.connection.DbConfigImpl;
import manifold.util.concurrent.LocklessLazyVar;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
public class SqlScopeFinder
{
  // Extension of JSON file[s] providing configuration for db connections
  public static final String DBCONFIG_EXT = "dbconfig";

  private final SqlManifold _sqlManifold;
  private final LocklessLazyVar<Set<SqlScope>> _scopes;

  SqlScopeFinder( SqlManifold sqlManifold )
  {
    _sqlManifold = sqlManifold;
    _scopes = LocklessLazyVar.make( () -> findScopes() );
  }

  Set<SqlScope> getScopes()
  {
    return _scopes.get();
  }

  SqlScope findScope( IFile sqlFile )
  {
    SqlScope sqlScope = _scopes.get().stream()
      .filter( scope -> scope.appliesTo( sqlFile ) )
      .findFirst().orElse( null );

    if( sqlScope != null )
    {
      return sqlScope;
    }

    if( SqlScope.isDefaultScopeApplicable( sqlFile ) )
    {
      return findDefaultScope();
    }

    return null;
  }

  private SqlScope findDefaultScope()
  {
    Set<SqlScope> scopes = getScopes();
    if( scopes.size() == 1 )
    {
      return scopes.stream().findFirst().get();
    }
    for( SqlScope scope : scopes )
    {
      if( scope.getDbconfig().isDefault() )
      {
        return scope;
      }
    }
    return null;
  }


  private Set<SqlScope> findScopes()
  {
    FqnCache<IFile> extensionCache = _sqlManifold.getModule().getPathCache().getExtensionCache( DBCONFIG_EXT );
    Set<SqlScope> scopes = new HashSet<>();
    extensionCache.visitDepthFirst(
      file ->
      {
        if( file != null )
        {
          scopes.add( makeScope( file ) );
        }
        return true;
      } );
    validate( scopes );
    return scopes;
  }

  private void validate( Set<SqlScope> scopes )
  {
    validateZeroOrOneDefaultScopes( scopes );
  }

  private static void validateZeroOrOneDefaultScopes( Set<SqlScope> scopes )
  {
    List<String> defaultScopes = scopes.stream()
      .filter( s -> s.getDbconfig().isDefault() )
      .map( s -> s.getDbconfig().getName() )
      .collect( Collectors.toList() );
    if( defaultScopes.size() > 1 )
    {
      StringBuilder defaultNamesList = new StringBuilder();
      for( String name : defaultScopes )
      {
        if( defaultNamesList.length() > 0 )
        {
          defaultNamesList.append( ", " );
        }
        defaultNamesList.append( name );
      }
      for( SqlScope s : scopes )
      {
        s.getIssues().add( new SqlIssue( IIssue.Kind.Warning, "Multiple default scopes found: " + defaultNamesList ) );
      }
    }
  }

  private SqlScope makeScope( IFile configFile )
  {
    return new SqlScope( _sqlManifold, configFile );
  }
}