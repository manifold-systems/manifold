package manifold.internal.javac;

/**
 */
public interface IIssue
{
  enum Kind { Error, Warning, Info, Failure, Other }

  Kind getKind();
  int getStartOffset();
  int getEndOffset();
  int getLine();
  int getColumn();
  String getMessage();
}
