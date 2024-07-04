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
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.api.Statement;
import manifold.sql.query.api.Command;
import manifold.sql.query.api.QueryTable;
import manifold.sql.query.api.SqlAnalyzer;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.schema.api.Schema;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SqlModel extends AbstractSingleFileModel
{
  private final SqlManifold _sqlManifold;
  private SqlScope _scope;
  private SqlParentType _type;
  private Statement _sqlStatement;
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
    _type = isCommand()
      ? new CommandParentType( this )
      : new QueryParentType( this );
  }

  public boolean isQuery()
  {
    return getSqlStatement() instanceof QueryTable;
  }
  public boolean isCommand()
  {
    return getSqlStatement() instanceof Command;
  }

  private void analyze()
  {
    if( _scope.isErrant() )
    {
      // the dbconfig has no driver, no way to analyze the SQL
      _issues = new SqlIssueContainer( DriverInfo.ERRANT, new ArrayList<>(), false );
      return;
    }

    String content = null;
    try( Reader reader = new InputStreamReader( getFile().openInputStream() ) )
    {
      SqlAnalyzer sqlAnalyzer = SqlAnalyzer.PROVIDERS.get().stream()
        .findFirst()
        .orElseThrow( () -> new RuntimeException( "Missing SqlAnalyzer provider" ) );
      content = StreamUtil.getContent( reader );
      _sqlStatement = sqlAnalyzer.makeStatement( ManClassUtil.getShortClassName( getFqn() ), _scope, content );
      _issues = _sqlStatement.getIssues();
    }
    catch( RuntimeException ise )
    {
      _sqlStatement = null;
      Schema schema = _scope.getSchema();
      _issues = new SqlIssueContainer( schema == null ? DriverInfo.ERRANT : schema.getDriverInfo(),
        Collections.singletonList( ise ), ManStringUtil.isCrLf( content ) );
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

  Statement getSqlStatement()
  {
    return _sqlStatement;
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

  void addIssue( IIssue.Kind kind, int offset, String msg )
  {
    _issues.addIssue( kind, offset, msg );
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
    return _issues.getIssues();
  }
}