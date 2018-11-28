package manifold.internal.javac;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.util.function.Supplier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import manifold.util.JavacDiagnostic;

/**
 */
public class IssueReporter<T> implements DiagnosticListener<T>
{
  private Supplier<Context> _context;

  public IssueReporter( Supplier<Context> context )
  {
    _context = context;
  }

  public void reportInfo( String msg )
  {
    report( (Diagnostic<? extends T>)new JavacDiagnostic( null, Diagnostic.Kind.NOTE, 0, 0, 0, msg ) );
  }

  public void reportWarning( String msg )
  {
    report( (Diagnostic<? extends T>)new JavacDiagnostic( null, Diagnostic.Kind.WARNING, 0, 0, 0, msg ) );
  }

  public void reportError( String msg )
  {
    report( (Diagnostic<? extends T>)new JavacDiagnostic( null, Diagnostic.Kind.ERROR, 0, 0, 0, msg ) );
  }

  public void report( Diagnostic<? extends T> diagnostic )
  {
    //## would rather use javax.annotation.processing.Messager, but it doesn't give us what we want
    Log issueLogger = Log.instance( _context.get() );

    IDynamicJdk.instance().report( issueLogger, diagnostic );
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
