/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.strings;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import manifold.api.util.StreamUtil;

public class ManDiagnosticHandler extends Log.DiagnosticHandler
{
  private Set<Integer> _escapedPos;
  private String _source;

  ManDiagnosticHandler( Context ctx )
  {
    install( Log.instance( ctx ) );
    _escapedPos = new HashSet<>();
  }

  boolean isEscapedPos( int pos )
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
