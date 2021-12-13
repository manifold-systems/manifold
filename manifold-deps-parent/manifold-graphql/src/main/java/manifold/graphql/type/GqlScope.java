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

import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.internal.javac.IIssue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class reflects the <i>.graphqlconfig</i> file, which defines the scope of a GraphQL schema in terms of where
 * the .graphql resources may be located.
 * <p/>
 * When no .graphqlconfig files are present, there can be only one GraphQL schema definition and the entire module's
 * namespace is the schema's scope -- all the module's .graphql resource files are considered part of the schema's
 * scope.
 * <p/>
 * When using multiple .grpahqlconfig files, it is advisable to place them in separate, unrelated directories alongside
 * the schema files. This way, each schema is scoped to the entire directory and its subdirectories.
 * <p/>
 * Note, no two .graphqlconfig files may overlap in terms of resource scoping.
 * @see <a href="https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#setting-up-multi-schema-projects-using-graphql-config">Setting up Multi-schema Projects using graphql-config</a>
 */
class GqlScope
{
  private static final String DEFAULT = "_default_scope";
  private static final String ERRANT = "_errant_scope";

  private static final Map<GqlManifold, GqlScope> DEFAULT_SCOPE_BY_MODULE = new WeakHashMap<>();
  static GqlScope getDefaultScope( GqlManifold gqlManifold ) {
    return DEFAULT_SCOPE_BY_MODULE.computeIfAbsent( gqlManifold, key -> new GqlScope( key ) );
  }

  private final GqlManifold _gqlManifold;
  private final IFile _configFile;
  private String _name;
  private final Set<File> _schemaFiles;
  private final List<String> _includes;
  private final List<String> _excludes;
  private SchemaDefinition _schemaDefinition;
  private final List<IIssue> _issues;

  GqlScope( GqlManifold gqlManifold, IFile configFile, String name, Set<String> schemaFiles, List<String> includes, List<String> excludes, List<IIssue> issues )
  {
    _gqlManifold = gqlManifold;
    _configFile = configFile;
    _name = name;
    _schemaFiles = schemaFiles.stream().map( s -> new File( s ) ).collect( Collectors.toSet() );
    _includes = includes;
    _excludes = excludes;
    _issues = issues;
  }

  /**
   * Default scope. Used when no .graphqlconfig files are present. Indicates all .graphql files in the module are in
   * scope. Note, this means there should be only ONE schema definition in the entire module. If you have multiple
   * schemas in the same module, you must use .graphqlconfig file[s] to define the scope for each schema.
   */
  private GqlScope( GqlManifold gqlManifold )
  {
    _gqlManifold = gqlManifold;
    _configFile = null;
    _name = DEFAULT;
    _schemaFiles = Collections.emptySet();
    _includes = Collections.emptyList();
    _excludes = Collections.emptyList();
    _issues = new ArrayList<>();
  }

  /**
   * Errant config.
   */
  private GqlScope( GqlManifold gqlManifold, IFile configFile )
  {
    _gqlManifold = gqlManifold;
    _configFile = configFile;
    _name = ERRANT;
    _schemaFiles = Collections.emptySet();
    _includes = Collections.emptyList();
    _excludes = Collections.emptyList();
    _issues = new ArrayList<>();
  }

  public static GqlScope makeErrantScope( GqlManifold gqlManifold, String fqn, IFile file )
  {
    GqlScope errantScope = new GqlScope( gqlManifold, file );
    errantScope._issues.add(
      new GqlIssue( IIssue.Kind.Error, 0, 0, 0,
        "GraphQL type '" + fqn + "' from file '" + file.getName() + "' is not covered in any .graphqlconfig files" ) );
    return errantScope;
  }

  boolean isDefault()
  {
    return DEFAULT.equals( _name );
  }

  boolean hasConfigErrors()
  {
    return _issues.stream().anyMatch( issue -> issue.getKind() == IIssue.Kind.Error );
  }

  IFile getConfigFile()
  {
    return _configFile;
  }

  String getName()
  {
    return _name;
  }
  void setName( String name )
  {
    _name = name;
  }

  SchemaDefinition getSchemaDefinition()
  {
    ensureSchemaDefinitionAssigned();
    return _schemaDefinition;
  }

  @Nullable
  private void ensureSchemaDefinitionAssigned()
  {
    if( _schemaDefinition == null )
    {
      if( !isDefault() )
      {
        for( File file : _schemaFiles )
        {
          IFile schemaFile = _gqlManifold.getModule().getHost().getFileSystem().getIFile( file );
          Set<String> fqnForFile = _gqlManifold.getModule().getPathCache().getFqnForFile( schemaFile );
          if( fqnForFile != null && !fqnForFile.isEmpty() )
          {
            _gqlManifold.getModel( fqnForFile.iterator().next() );
            if( _schemaDefinition != null )
            {
              return;
            }
          }
        }
      }

      // default scope,
      // or schema files from config file don't have the schema def, force all .graphql files to load models to find it
      _gqlManifold.findByModel( model -> null );
    }
  }

  void setSchemaDefinition( SchemaDefinition schemaDefinition )
  {
    _schemaDefinition = schemaDefinition;
  }

  List<IIssue> getIssues()
  {
    return _issues;
  }

  TypeDefinition findTypeDefinition( String simpleName )
  {
    return _gqlManifold.findByModel(
      model -> contains( model.getFile() )
      ? model.getTypeDefinition( simpleName )
      : null );
  }

  ScalarTypeDefinition findScalarTypeDefinition( String simpleName )
  {
    return _gqlManifold.findByModel(
      model -> contains( model.getFile() )
        ? model.getScalarTypeDefinition( simpleName )
        : null );
  }

  Set<GqlModel> getAllModels()
  {
    return _gqlManifold.findAllByModel(
      model -> contains( model.getFile() )
        ? model
        : null )
      .collect( Collectors.toSet() );
  }

  /**
   * See https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#project-structure-and-schema-discovery.
   * <p/>
   * See https://github.com/kamilkisiela/graphql-config/blob/master/config-schema.json.
   */
  boolean contains( IFile file )
  {
    if( hasConfigErrors() )
    {
      return false;
    }

    if( isDefault() )
    {
      // implies a single, global scope which is the entire module
      return true;
    }

    if( file instanceof IFileFragment )
    {
      return getName() != null &&
        getName().equals( ((IFileFragment)file).getScope() );
    }

    if( _schemaFiles.stream()
      .anyMatch( f -> configFileRelativePath( f.getPath() )
        .equals( file.getPath().getFileSystemPathString() ) ) )
    {
      return true;
    }

    try
    {
      URI uri = new URI( file.toURI().getScheme() + ":/" );
      FileSystem fs = FileSystems.getFileSystem( uri );
      Path path = file.toJavaFile().toPath();
      if( !_includes.isEmpty() )
      {
        return _includes.stream().anyMatch(
          glob -> fs.getPathMatcher( configFileRelativePath( glob ) )
            .matches( path ) );
      }
      if( !_excludes.isEmpty() )
      {
        return _excludes.stream().noneMatch(
          glob -> fs.getPathMatcher( configFileRelativePath( glob ) )
            .matches( path ) );
      }
    }
    catch( URISyntaxException e )
    {
      e.printStackTrace();
    }

    // no includes or excludes implies all other graphql files in the same directory or subdirectories of the schema
    // files are in scope
    return _schemaFiles.stream()
      .anyMatch( f -> file.isDescendantOf(
        _gqlManifold.getModule().getHost().getFileSystem().getIDirectory( new File( configFileRelativePath( f.getPath() ) ).getParentFile() ) ) );
  }

  @NotNull
  private String configFileRelativePath( String path )
  {
    return _configFile.getParent().file( path ).toJavaFile().getAbsolutePath();
  }
}
