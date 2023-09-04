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
import manifold.api.util.cache.FqnCache;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.DbLocationProvider;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.impl.DbConfigImpl;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaProvider;
import manifold.sql.schema.api.SchemaTable;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
      Function<String, FqnCache<IFile>> resByExt = ext ->
        _schemaManifold.getModule().getPathCache().getExtensionCache( ext );
      _dbConfig = new DbConfigImpl( resByExt, bindings, DbLocationProvider.Mode.CompileTime );
      validate();
      for( SchemaProvider sp : SchemaProvider.PROVIDERS.get() )
      {
        try
        {
          Schema schema = sp.getSchema( _dbConfig );
          if( schema != null )
          {
            return schema;
          }
        }
        catch( Exception e )
        {
          _issues = new SchemaIssueContainer( Collections.singletonList( e ) );
        }
      }
    }
    catch( IOException e )
    {
      _issues = new SchemaIssueContainer( Collections.singletonList( e ) );
    }
    return null;
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
    if( _dbConfig == null )
    {
      // likely due to parse errors in the dbconfig file e.g., during editing in an IDE
      return;
    }

    super.updateFile( file );
    Dependencies.instance().getConnectionProvider().closeDataSource( _dbConfig );
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

  public SchemaIssueContainer getIssueContainer()
  {
    return _issues;
  }
}