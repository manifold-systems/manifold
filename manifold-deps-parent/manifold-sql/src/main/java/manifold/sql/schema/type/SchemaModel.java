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

package manifold.sql.schema.type;

import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.type.AbstractSingleFileModel;
import manifold.api.util.JavacDiagnostic;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.DbLocationProvider;
import manifold.sql.rt.connection.DbConfigImpl;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaProvider;
import manifold.sql.schema.api.SchemaTable;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Set;

public class SchemaModel extends AbstractSingleFileModel
{
  private final SchemaManifold _schemaManifold;
  private LocklessLazyVar<Schema> _schema;
  private DbConfigImpl _dbConfig;
  private SchemaParentType _type;
  private SchemaIssueContainer _issues;

  @SuppressWarnings( "WeakerAccess" )
  public SchemaModel( SchemaManifold schemaManifold, String fqn, Set<IFile> files )
  {
    super( schemaManifold.getModule().getHost(), fqn, files );
    _schemaManifold = schemaManifold;
    init();
  }

  private void init()
  {
    _issues = null;
    _schema = LocklessLazyVar.make( () -> loadSchema() );
    _type = new SchemaParentType( this );
  }

  private Schema loadSchema()
  {
    try( Reader reader = new InputStreamReader( getFile().openInputStream() ) )
    {
      Bindings bindings = (Bindings)Json.fromJson( StreamUtil.getContent( reader ) );
      bindings.put( "name", getFile().getBaseName() );
      bindings.put( "path", getFile().getPath().getFileSystemPathString() );
      _dbConfig = new DbConfigImpl( bindings, DbLocationProvider.Mode.CompileTime );
      validate();
      return SchemaProvider.PROVIDERS.get().stream()
        .map( sp -> sp.getSchema( _dbConfig ) )
        .filter( schema -> schema != null )
        .findFirst().orElse( null );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private void validate()
  {
    _issues = new SchemaIssueContainer();
    String url = _dbConfig.getUrl();
    if( url == null || url.isEmpty() )
    {
      _issues.addIssue( IIssue.Kind.Error, "Required \"url\" entry is missing from dbconfig: " + getFile().getName() );
    }
    String schemaPackage = _dbConfig.getSchemaPackage();
    if( schemaPackage == null || schemaPackage.isEmpty() )
    {
      _issues.addIssue( IIssue.Kind.Error, "Required \"schemaPackage\" entry is missing from dbconfig: " + getFile().getName() );
    }
  }

  DbConfig getDbConfig()
  {
    return _dbConfig;
  }

  Schema getSchema()
  {
    return _schema.get();
  }

  SchemaParentType getType()
  {
    return _type;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    ConnectionProvider.findFirst().closeDataSource( _dbConfig );
    init();
  }

  SchemaTable getTable( String simpleName )
  {
    Schema schema = getSchema();
    return schema.getTable( schema.getOriginalName( simpleName ) );
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( errorHandler == null )
    {
      return;
    }

    List<IIssue> issues = _issues.getIssues();
    if( !issues.isEmpty() )
    {
      JavaFileObject file = new SourceJavaFileObject( getFile().toURI() );
      for( IIssue issue : issues )
      {
        int offset = issue.getStartOffset();
        if( getFile() instanceof IFileFragment )
        {
          offset += ((IFileFragment)getFile()).getOffset();
        }
        Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        errorHandler.report( new JavacDiagnostic( file, kind, offset, issue.getLine(), issue.getColumn(), issue.getMessage() ) );
      }
    }
  }

  SchemaIssueContainer getIssueContainer()
  {
    return _issues;
  }
}