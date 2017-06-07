package manifold.internal.javac;

import java.util.List;

/**
 */
public interface IIssueContainer
{
  List<IIssue> getIssues();
  List<IIssue> getWarnings();
  List<IIssue> getErrors();

  boolean isEmpty();
}
