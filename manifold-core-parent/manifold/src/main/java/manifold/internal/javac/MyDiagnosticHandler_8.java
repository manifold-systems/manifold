package manifold.internal.javac;

import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;

public class MyDiagnosticHandler_8 extends Log.DiagnosticHandler
{
  MyDiagnosticHandler_8( Log log )
  {
    install( log );
  }

  @Override
  public void report( JCDiagnostic jcDiagnostic )
  {
    prev.report( jcDiagnostic );
  }
}
