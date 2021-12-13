/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.graphql.type;

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;
import manifold.internal.javac.IIssue;
import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.StreamUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/**
 * Using graphql config file format from https://github.com/kamilkisiela/graphql-config/blob/master/config-schema.json
 */
public class GqlScopeFinder
{
  // Extension of JSON file[s] providing configuration for GraphQL schema[s]
  public static final String GRAPHQLCONFIG_EXT = "graphqlconfig";

  private final GqlManifold _gqlManifold;
  private final LocklessLazyVar<Set<GqlScope>> _scopes;

  GqlScopeFinder( GqlManifold gqlManifold )
  {
    _gqlManifold = gqlManifold;
    _scopes = LocklessLazyVar.make( () -> findScopes() );
  }

  Set<GqlScope> getScopes()
  {
    return _scopes.get();
  }

  GqlScope findScope( IFile file )
  {
    return _scopes.get().stream()
      .filter( scope -> scope.contains( file ) )
      .findFirst().orElse( null );
  }

  private Set<GqlScope> findScopes()
  {
    FqnCache<IFile> extensionCache = _gqlManifold.getModule().getPathCache().getExtensionCache( GRAPHQLCONFIG_EXT );
    Set<GqlScope> scopes = new HashSet<>();
    extensionCache.visitDepthFirst(
      file ->
      {
        if( file != null )
        {
          scopes.addAll( makeScope( file ) );
        }
        return true;
      } );
    if( scopes.isEmpty() )
    {
      scopes.add( GqlScope.getDefaultScope( _gqlManifold ) );
    }
    return scopes;
  }

  private Set<GqlScope> makeScope( IFile configFile )
  {
    try( Reader reader = new InputStreamReader( configFile.openInputStream() ) )
    {
      Bindings bindings = (Bindings)Json.fromJson( StreamUtil.getContent( reader ) );
      return makeScopes( bindings, configFile );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private Set<GqlScope> makeScopes( Bindings bindings, IFile configFile )
  {
    Set<GqlScope> scopes = new HashSet<>();

    Set<String> schemaFiles = new HashSet<>();
    List<String> includes = new ArrayList<>();
    List<String> excludes = new ArrayList<>();
    List<IIssue> issues = new ArrayList<>();

    if( bindings.containsKey( "schemaPath" ) )
    {
      // "legacy" config file format

      schemaFiles.add( (String)bindings.get( "schemaPath" ) );
      String name = (String)bindings.get( "name" );

      validateConfigName( name, issues );
      schemaFiles.forEach( s -> validateSchemaName( configFile, s, issues ) );
      scopes.add( new GqlScope( _gqlManifold, configFile, name, schemaFiles, includes, excludes, issues ) );
    }
    else if( bindings.containsKey( "schema" ) )
    {
      // newer config file format

      Object schema = bindings.get( "schema" );
      if( schema instanceof String )
      {
        schemaFiles.add( (String)schema );
      }
      else if( schema instanceof List )
      {
        //noinspection unchecked
        schemaFiles.addAll( (List<String>)schema );
      }
      else
      {
        issues.add( new GqlIssue( IIssue.Kind.Error, 0, 0, 0,
          "Illegal .graphqlconfig format. Expecting string or array for schema[s], found " + schema.getClass().getSimpleName() ) );
      }

      schemaFiles.forEach( s -> validateSchemaName( configFile, s, issues ) );

      String name = (String)bindings.get( "name" );
      validateConfigName( name, issues );
      scopes.add( new GqlScope( _gqlManifold, configFile, name, schemaFiles, includes, excludes, issues ) );
    }
    else if( bindings.containsKey( "projects" ) )
    {
      // multiple scopes (aka projects) in newer config format
      
      //noinspection unchecked
      for( Bindings project : (List<Bindings>)bindings.get( "projects" ) )
      {
        for( Map.Entry entry: project.entrySet() )
        {
          Set<GqlScope> gqlScopes = makeScopes( project, configFile );
          if( !gqlScopes.isEmpty() )
          {
            String name = (String)entry.getKey();
            validateConfigName( name, issues );
            gqlScopes.iterator().next().setName( name );
          }
        }
      }
    }
    return scopes;
  }

  private void validateSchemaName( IFile configFile, String schemaFile, List<IIssue> issues )
  {
    if( !configFile.getParent().file( schemaFile ).exists() )
    {
      issues.add( new GqlIssue( IIssue.Kind.Error, 0, 0, 0, "Schema path '" + schemaFile + "' does not exist." ) );
    }
  }

  private void validateConfigName( String name, List<IIssue> issues )
  {
    if( name != null )
    {
      if( !ManClassUtil.isJavaIdentifier( name ) )
      {
        issues.add( new GqlIssue( IIssue.Kind.Warning, 0, 0, 0, "\"name\" must be a valid Java identifier, otherwise Manifold fragments cannot be used with the config." ) );
      }
    }
  }
}
