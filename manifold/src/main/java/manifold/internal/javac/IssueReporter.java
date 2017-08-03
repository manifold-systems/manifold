package manifold.internal.javac;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 */
public class IssueReporter<T> implements DiagnosticListener<T>
{
  //## would rather use javax.annotation.processing.Messager, but it doesn't give us what we want
  private Log _issueLogger;

  public IssueReporter( Log issueLogger )
  {
    _issueLogger = issueLogger;
  }

  public void report( Diagnostic<? extends T> diagnostic )
  {
    // Adapted from JavacMessager.printMessage.  Following same basic routine regarding use of Log

    JavaFileObject oldSource = _issueLogger.useSource( (JavaFileObject)diagnostic.getSource() );
    boolean oldMultipleErrors = _issueLogger.multipleErrors;
    _issueLogger.multipleErrors = true;
    try
    {
      switch( diagnostic.getKind() )
      {
        case ERROR:
          _issueLogger.error( new Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
        case WARNING:
          _issueLogger.warning( new Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
        case MANDATORY_WARNING:
          _issueLogger.mandatoryWarning( new Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
        case NOTE:
        case OTHER:
          _issueLogger.note( new Position( diagnostic ), "proc.messager", diagnostic.getMessage( Locale.getDefault() ) );
          break;
      }
    }
    finally
    {
      _issueLogger.useSource( oldSource );
      _issueLogger.multipleErrors = oldMultipleErrors;
    }
  }

  static class Position implements JCDiagnostic.DiagnosticPosition
  {
    private final Diagnostic _d;

    public Position( Diagnostic d )
    {
      _d = d;
    }

    @Override
    public JCTree getTree()
    {
      return null;
    }

    @Override
    public int getStartPosition()
    {
      return (int)_d.getStartPosition();
    }

    @Override
    public int getPreferredPosition()
    {
      return getStartPosition();
    }

    @Override
    public int getEndPosition( EndPosTable endPosTable )
    {
      return (int)_d.getEndPosition();
    }
  }

}
