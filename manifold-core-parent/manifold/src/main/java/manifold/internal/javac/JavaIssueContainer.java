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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 */
public class JavaIssueContainer implements IIssueContainer
{
  private DiagnosticCollector<JavaFileObject> _errorHandler;
  private List<IIssue> _issues;

  public JavaIssueContainer( DiagnosticCollector<JavaFileObject> errorHandler )
  {
    _errorHandler = errorHandler;
  }

  @Override
  public List<IIssue> getIssues()
  {
    if( _issues == null )
    {
      List<IIssue> issues = new ArrayList<>();
      if( _errorHandler != null )
      {
        for( Diagnostic diagnostic : _errorHandler.getDiagnostics() )
        {
          JavaIssue issue = new JavaIssue( diagnostic );
          issues.add( issue );
        }
      }
      _issues = issues;
    }

    return _issues;
  }

  @Override
  public List<IIssue> getWarnings()
  {
    return getIssues().stream().filter( issue -> issue.getKind() == IIssue.Kind.Warning ).collect( Collectors.toList() );
  }

  @Override
  public List<IIssue> getErrors()
  {
    return getIssues().stream().filter( issue -> issue.getKind() == IIssue.Kind.Error ).collect( Collectors.toList() );
  }

  @Override
  public boolean isEmpty()
  {
    return getIssues().isEmpty();
  }
}
