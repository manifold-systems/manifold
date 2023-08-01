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
import manifold.api.fs.IFileFragment;
import manifold.api.host.IModule;
import manifold.api.type.ITypeManifold;
import manifold.internal.javac.IIssue;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.connection.DbConfigImpl;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.type.SchemaIssueContainer;
import manifold.sql.schema.type.SchemaManifold;
import manifold.sql.schema.type.SchemaModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SqlScope
{
  private final IModule _module;
  private final List<IIssue> _issues;
  private final IFile _dbConfigFile;

  SqlScope( IModule module, IFile dbConfigFile )
  {
    _module = module;
    _dbConfigFile = dbConfigFile;
    _issues = new ArrayList<>();
  }

  private Schema findSchema( IFile dbConfigFile )
  {
    // share the schema from the corresponding SchemaManifold

    Set<ITypeManifold> tms = _module.findTypeManifoldsFor( dbConfigFile );
    SchemaManifold schemaManifold = (SchemaManifold)tms.stream()
      .filter( m -> m instanceof SchemaManifold )
      .findFirst()
      .orElseThrow( () -> new RuntimeException( "Could not find schema manifold for: " + dbConfigFile.getName() ) );
    addIssuesFromSchemaModel( dbConfigFile, schemaManifold );
    return schemaManifold.getSchema( dbConfigFile );

// don't make a separate schema, it is costly (new jdbc connection + full database metadata extraction)
//    return SchemaProvider.PROVIDERS.get().stream().map( sp -> sp.getSchema( _dbConfig ) ).filter( schema -> schema != null ).findFirst().orElse( null );
  }

  private void addIssuesFromSchemaModel( IFile dbConfigFile, SchemaManifold schemaManifold )
  {
    Set<String> fqnForFile = schemaManifold.getModule().getPathCache().getFqnForFile( dbConfigFile );
    if( !fqnForFile.isEmpty() )
    {
      SchemaModel schemaModel = schemaManifold.getModel( schemaManifold.getTypeNameForFile( fqnForFile.iterator().next(), dbConfigFile ) );
      SchemaIssueContainer issueContainer = schemaModel == null ? null : schemaModel.getIssueContainer();
      if( issueContainer != null && !issueContainer.isEmpty() )
      {
        _issues.addAll( issueContainer.getIssues() );
      }
    }
  }

  /**
   * Errant config.
   */
  private SqlScope( IModule module )
  {
    _module = module;
    _issues = new ArrayList<>();
    _dbConfigFile = null;
  }

  public static SqlScope makeErrantScope( IModule module, String fqn, IFile file )
  {
    SqlScope errantScope = new SqlScope( module );
    errantScope._issues.add( new SqlIssue( IIssue.Kind.Error, 0, "SQL type '" + fqn + "' from file '" + file.getName() + "' is not covered in any .dbconfig files" ) );
    return errantScope;
  }

  boolean hasConfigErrors()
  {
    return _issues.stream().anyMatch( issue -> issue.getKind() == IIssue.Kind.Error );
  }

  public DbConfig getDbconfig()
  {
    Schema schema = getSchema();
    return schema == null ? DbConfigImpl.EMPTY : getSchema().getDbConfig();
  }

  public Schema getSchema()
  {
    return _dbConfigFile == null ? null : findSchema( _dbConfigFile );
  }

  List<IIssue> getIssues()
  {
    return _issues;
  }

  boolean appliesTo( IFile file )
  {
    if( hasConfigErrors() || getDbconfig().getName() == null )
    {
      return false;
    }

    String dbConfigName = findDbConfigName( file );
    if( dbConfigName != null )
    {
      return getDbconfig().getName().equals( dbConfigName );
    }

    return false;
  }

  public static boolean isDefaultScopeApplicable( IFile file )
  {
    String dbConfigName = findDbConfigName( file );
    return dbConfigName == null || dbConfigName.isEmpty();
  }

  static String findDbConfigName( IFile file )
  {
    if( file instanceof IFileFragment )
    {
      return ((IFileFragment)file).getScope();
    }

    // look for name like: MyQuery.MyDbConfigName.sql

    String fileBaseName = file.getBaseName();
    int dbconfigName = fileBaseName.lastIndexOf( '.' );
    if( dbconfigName < 0 )
    {
      // No secondary extension in name
      return null;
    }
    return fileBaseName.substring( dbconfigName + 1 );
  }

  public boolean isErrant()
  {
    return getDbconfig().getBuildUrlOtherwiseRuntimeUrl() == null;
  }

  /**
   * When in an IDE, PathCache mappings for dbconfig files will contain two entries, one with a fqn mirroring the resource
   * path, the other mirroring the schema package (the actual type name of the generated dbconfig schema class). Ultimately,
   * this causes two SqlScope instances to exist in the PathCache, both referring to the same dbconfig. Therefore, the
   * equals/hashCode impls must account for this so that Sets don't contain duplicates e.g., to fulfill the rule about
   * if one dbconfig exists, it is the considered the default. (two of the same dbconfig breaks this)
   */
  @Override
  public boolean equals( Object o )
  {
    if( this == o ) return true;
    if( !(o instanceof SqlScope) ) return false;
    SqlScope sqlScope = (SqlScope)o;
    return _dbConfigFile != null && sqlScope._dbConfigFile != null &&
      Objects.equals( _dbConfigFile.getPath(), sqlScope._dbConfigFile.getPath() );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _dbConfigFile );
  }
}