package manifold.api.json;

import manifold.internal.javac.IIssue;

/**
 */
public class JsonIssue extends RuntimeException implements IIssue
{
  private Kind _kind;
  private final int _offset;
  private int _line;
  private int _column;
  private String _msg;

  public JsonIssue( Kind kind, Token token, String msg )
  {
    this( kind,
          token == null ? 0 : token.getOffset(),
          token == null ? 0 : token.getLineNumber(),
          token == null ? 0 : token.getColumn(),
          msg );
  }
  public JsonIssue( Kind kind, int offset, int line, int column, String msg )
  {
    super( msg );
    _kind = kind;
    _offset = offset;
    _line = line;
    _column = column;
    _msg = msg;
  }

  @Override
  public Kind getKind()
  {
    return _kind;
  }

  @Override
  public int getStartOffset()
  {
    return _offset;
  }

  @Override
  public int getEndOffset()
  {
    return _offset;
  }

  @Override
  public int getLine()
  {
    return _line;
  }

  @Override
  public int getColumn()
  {
    return _column;
  }

  @Override
  public String getMessage()
  {
    return _msg;
  }
}
