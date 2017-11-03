package manifold.api.json;

import java.net.MalformedURLException;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.type.AbstractSingleFileModel;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.util.JavacDiagnostic;

/**
 */
class JsonModel extends AbstractSingleFileModel
{
  private IJsonParentType _type;
  private JsonIssueContainer _issues;

  public JsonModel( String fqn, Set<IFile> files )
  {
    super( fqn, files );
    init();
  }

  private void init()
  {
    Bindings bindings;
    try
    {
      bindings = Json.fromJson( ResourceFileTypeManifold.getContent( getFile() ), false, true );
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

    try
    {
      _type = (IJsonParentType)Json.transformJsonObject( getFile().getBaseName(), getFile().toURI().toURL(), null, bindings );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  public IJsonParentType getType()
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