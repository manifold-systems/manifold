/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.templates.manifold;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.type.AbstractSingleFileModel;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.templates.codegen.TemplateGen;
import manifold.util.JavacDiagnostic;
import manifold.util.StreamUtil;

class TemplateModel extends AbstractSingleFileModel
{
  private String _source;
  private TemplateIssueContainer _issues;

  TemplateModel( IManifoldHost host, String fqn, Set<IFile> files )
  {
    super( host, fqn, files );
    init();
  }

  private void init()
  {
    IFile file = getFile();
    try
    {
      String templateSource = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
      templateSource = templateSource.replace( "\r\n", "\n" );
      TemplateGen generator = new TemplateGen();
      _source = generator.generateCode( getFqn(), templateSource, file.toURI(), file.getName() );
      _issues = generator.getIssues();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }

  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    init();
  }

  String getSource()
  {
    return _source;
  }

  void report( DiagnosticListener errorHandler )
  {
    if( _issues.isEmpty() || errorHandler == null )
    {
      return;
    }

    JavaFileObject file = new SourceJavaFileObject( getFile().toURI() );
    for( IIssue issue: _issues.getIssues() )
    {
      Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
      errorHandler.report( new JavacDiagnostic( file, kind, issue.getStartOffset(), issue.getLine(), issue.getColumn(), issue.getMessage() ) );
    }
  }

}
