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

package manifold.api.json;

import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.type.AbstractSingleFileModel;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.util.JavacDiagnostic;

/**
 */
class Model extends AbstractSingleFileModel
{
  private JsonStructureType _type;
  private JsonIssueContainer _issues;

  Model( IManifoldHost host, String fqn, Set<IFile> files )
  {
    super( host, fqn, files );
    init();
  }

  private void init()
  {
    Bindings bindings;
    try
    {
      bindings = Json.fromJson( ResourceFileTypeManifold.getContent( getFile() ) );
    }
    catch( Exception e )
    {
      Throwable cause = e.getCause();
      if( cause instanceof ScriptException )
      {
        _issues = new JsonIssueContainer( (ScriptException)cause, getFile() );
      }
      bindings = new SimpleBindings();
    }

    IJsonType type = Json.transformJsonObject( getHost(), getFile().getBaseName(), null, bindings );
    _type = getStructureType( type );
  }

  private JsonStructureType getStructureType( IJsonType type )
  {
    if( type instanceof JsonListType )
    {
      return getStructureType( ((JsonListType)type).getComponentType() );
    }
    if( type instanceof JsonStructureType )
    {
      return (JsonStructureType)type;
    }
    return null;
  }

  public JsonStructureType getType()
  {
    return _type;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    init();
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( _issues == null || errorHandler == null )
    {
      return;
    }

    JavaFileObject file = new SourceJavaFileObject( getFile().toURI() );
    for( IIssue issue : _issues.getIssues() )
    {
      Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
      errorHandler.report( new JavacDiagnostic( file, kind, issue.getStartOffset(), issue.getLine(), issue.getColumn(), issue.getMessage() ) );
    }
  }

}
