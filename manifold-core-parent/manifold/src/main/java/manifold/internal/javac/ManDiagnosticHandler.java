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
  private String _source;

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
      String source = getSource( jcDiagnostic );
      int pos = (int)jcDiagnostic.getPosition();
      char escaped = source.charAt( pos );
      if( escaped == '$' )
      {
        _escapedPos.add( pos );
        return;
      }
    }

    prev.report( jcDiagnostic );
  }

  private String getSource( JCDiagnostic jcDiagnostic )
  {
    if( _source == null )
    {
      try
      {
        Reader reader = jcDiagnostic.getSource().openReader( true );
        _source = StreamUtil.getContent( reader );
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }
    return _source;
  }
}
