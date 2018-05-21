package manifold.internal.javac;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import manifold.util.StreamUtil;

public class ManDiagnosticHandler extends Log.DiagnosticHandler
{
  private Set<Integer> _escapedPos;

  public ManDiagnosticHandler( Context ctx )
  {
    install( Log.instance( ctx ) );
    _escapedPos = new HashSet<>();
  }

  public boolean isEscapedPos( int pos )
  {
    return _escapedPos.contains( pos );
  }

  @Override
  public void report( JCDiagnostic jcDiagnostic )
  {
    if( jcDiagnostic. getCode().equals( "compiler.err.illegal.esc.char" ) )
    {
      try
      {
        Reader reader = jcDiagnostic.getSource().openReader( true );
        int pos = (int)jcDiagnostic.getPosition();
        char escaped = StreamUtil.getContent( reader ).charAt( pos );
        if( escaped == '$' )
        {
          _escapedPos.add( pos );
          return;
        }
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }

    prev.report( jcDiagnostic );
  }
}
