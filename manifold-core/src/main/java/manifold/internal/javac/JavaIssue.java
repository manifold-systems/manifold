package manifold.internal.javac;

import java.util.Locale;
import javax.tools.Diagnostic;

/**
 */
public class JavaIssue implements IIssue
{
  private final Diagnostic _diagnostic;

  public JavaIssue( Diagnostic diagnostic )
  {
    _diagnostic = diagnostic;
  }

  @Override
  public Kind getKind()
  {
    return _diagnostic.getKind() == Diagnostic.Kind.ERROR
           ? Kind.Error
           : Kind.Warning;
  }

  @Override
  public int getStartOffset()
  {
    return (int)_diagnostic.getStartPosition();
  }

  @Override
  public int getEndOffset()
  {
    return (int)_diagnostic.getEndPosition();
  }

  @Override
  public int getLine()
  {
    return (int)_diagnostic.getLineNumber();
  }

  @Override
  public int getColumn()
  {
    return (int)_diagnostic.getColumnNumber();
  }

  @Override
  public String getMessage()
  {
    return _diagnostic.getMessage( Locale.getDefault() );
  }
}
