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
