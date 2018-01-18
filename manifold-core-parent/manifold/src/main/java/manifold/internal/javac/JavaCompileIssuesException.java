package manifold.internal.javac;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 */
public class JavaCompileIssuesException extends RuntimeException
{
  private DiagnosticCollector<JavaFileObject> _errorHandler;

  public JavaCompileIssuesException( String fqn, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    super( makeMessage( fqn, errorHandler ) );
    _errorHandler = errorHandler;
  }

  public DiagnosticCollector<JavaFileObject> getErrorHandler()
  {
    return _errorHandler;
  }

  private static String makeMessage( String fqn, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder( "Error compiling Java class: " + fqn + "\n" );
    if( errorHandler == null || errorHandler.getDiagnostics() == null )
    {
      return sb.append( "No error messages available" ).toString();
    }
    sb.append( "\n" );
    for( Diagnostic d : errorHandler.getDiagnostics() )
    {
      sb.append( d.toString() ).append( "\n" );
    }
    return sb.toString();
  }
}
