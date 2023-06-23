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
import manifold.internal.javac.IIssue;
import manifold.json.rt.api.DataBindings;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.connection.DbConfigImpl;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SqlScope
{
  private static final String ERRANT = "_errant_scope";

  private final SqlManifold _sqlManifold;
  private final DbConfig _dbConfig;
  private final List<IIssue> _issues;
  private final Schema _schema;

  SqlScope( SqlManifold sqlManifold, DbConfig dbConfig, List<IIssue> issues )
  {
    _sqlManifold = sqlManifold;
    _dbConfig = dbConfig;
    _issues = issues;
    _schema = findSchema();
  }

  private Schema findSchema()
  {
    return SchemaProvider.PROVIDERS.get().stream().map( sp -> sp.getSchema( _dbConfig ) ).filter( schema -> schema != null ).findFirst().orElse( null );
  }

  /**
   * Errant config.
   */
  private SqlScope( SqlManifold sqlManifold, IFile configFile )
  {
    _sqlManifold = sqlManifold;
    _dbConfig = new DbConfigImpl( DataBindings.EMPTY_BINDINGS );
    _issues = new ArrayList<>();
    _schema = null;
  }

  public static SqlScope makeErrantScope( SqlManifold sqlManifold, String fqn, IFile file )
  {
    SqlScope errantScope = new SqlScope( sqlManifold, file );
    errantScope._issues.add( new SqlIssue( IIssue.Kind.Error, "SQL type '" + fqn + "' from file '" + file.getName() + "' is not covered in any .dbconfig files" ) );
    return errantScope;
  }

  boolean hasConfigErrors()
  {
    return _issues.stream().anyMatch( issue -> issue.getKind() == IIssue.Kind.Error );
  }

  public DbConfig getDbconfig()
  {
    return _dbConfig;
  }

  public Schema getSchema()
  {
    return _schema;
  }

  List<IIssue> getIssues()
  {
    return _issues;
  }

  boolean contains( IFile file )
  {
    if( hasConfigErrors() )
    {
      return false;
    }

    if( file instanceof IFileFragment )
    {
      return _dbConfig.getName() != null && _dbConfig.getName().equals( ((IFileFragment)file).getScope() );
    }

    // sql files in the same directory or subdirectories of the config file are in scope
    return file.isDescendantOf( _sqlManifold.getModule().getHost().getFileSystem().getIDirectory( new File( _dbConfig.getPath() ).getParentFile() ) );
  }

  public boolean isErrant()
  {
    return getDbconfig().getBuildUrlOtherwiseRuntimeUrl() == null;
  }
}