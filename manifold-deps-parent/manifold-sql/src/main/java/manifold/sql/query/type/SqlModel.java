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
import manifold.api.type.AbstractSingleFileModel;
import manifold.api.util.JavacDiagnostic;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.query.api.QueryAnalyzer;
import manifold.sql.query.api.QueryTable;
import manifold.util.ManExceptionUtil;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SqlModel extends AbstractSingleFileModel
{
  private final SqlManifold _sqlManifold;
  private SqlScope _scope;
  private SqlParentType _type;
  private QueryTable _query;
  private SqlIssueContainer _issues;

  @SuppressWarnings("WeakerAccess")
  public SqlModel( SqlManifold sqlManifold, String fqn, Set<IFile> files )
  {
    super( sqlManifold.getModule().getHost(), fqn, files );
    _sqlManifold = sqlManifold;
    init();
  }

  private void init()
  {
    _issues = null;
    _scope = assignScope();
    analyze();
    _type = new SqlParentType( this );
  }

  private void analyze()
  {
    try( Reader reader = new InputStreamReader( getFile().openInputStream() ) )
    {
      QueryAnalyzer queryAnalyzer = QueryAnalyzer.PROVIDERS.get().stream()
        .findFirst()
        .orElseThrow( () -> new RuntimeException( "Missing QueryAnalyzer provider" ) );
      _query = queryAnalyzer.getQuery( ManClassUtil.getShortClassName( getFqn() ), _scope,
        StreamUtil.getContent( reader ) );
      _issues = _query.getIssues();
    }
    catch( RuntimeException ise )
    {
      _query = null;
      _issues = new SqlIssueContainer( Collections.singletonList( ise ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private SqlScope assignScope()
  {
    SqlScope scope = _sqlManifold.getScopeFinder().findScope( getFile() );
    if( scope == null )
    {
      scope = SqlScope.makeErrantScope( _sqlManifold.getModule(), getFqn(), getFile() );
    }
    return scope;
  }

  SqlScope getScope()
  {
    return _scope;
  }

  QueryTable getQuery()
  {
    return _query;
  }

  SqlParentType getType()
  {
    return _type;
  }

  IModule getModule()
  {
    return _sqlManifold.getModule();
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    init();
  }

  void addIssue( Exception issue )
  {
    _issues.addIssues( Collections.singletonList( issue ) );
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( errorHandler == null )
    {
      return;
    }

    List<IIssue> scopeIssues = getScope().getIssues();
    if( !scopeIssues.isEmpty() )
    {
//      IFile scopeConfigFile = getScope().getDbconfig();
//      JavaFileObject configFile = scopeConfigFile == null ? null : new SourceJavaFileObject( scopeConfigFile.toURI() );
      for( IIssue scopeIssue : scopeIssues )
      {
        Diagnostic.Kind kind = scopeIssue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        errorHandler.report( new JavacDiagnostic( null, kind, scopeIssue.getStartOffset(), scopeIssue.getLine(), scopeIssue.getColumn(), scopeIssue.getMessage() ) );
      }
    }

    List<IIssue> issues = getIssues();
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

  private List<IIssue> getIssues()
  {
    List<IIssue> allIssues = new ArrayList<>();
    if( _issues != null )
    {
      allIssues.addAll( _issues.getIssues() );
    }
    return allIssues;
  }
}