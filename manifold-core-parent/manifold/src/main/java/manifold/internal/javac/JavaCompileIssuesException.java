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
