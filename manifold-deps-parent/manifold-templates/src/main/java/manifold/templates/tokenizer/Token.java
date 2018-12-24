package manifold.templates.tokenizer;


public class Token
{
  public enum TokenType
  {
    CONTENT,
    COMMENT,
    EXPR,
    STMT,
    DIRECTIVE,
    EXPR_BRACE_BEGIN( "${" ),
    EXPR_BRACE_END( "}" ),
    EXPR_ANGLE_BEGIN( "<%=" ),
    STMT_ANGLE_BEGIN( "<%" ),
    DIR_ANGLE_BEGIN( "<%@" ),
    ANGLE_END( "%>" ),
    COMMENT_BEGIN( "<%--" ),
    COMMENT_END( "--%>" );

    private String _staticToken;

    TokenType()
    {
    }

    TokenType( String staticToken )
    {
      _staticToken = staticToken;
    }

    public String getToken()
    {
      return _staticToken;
    }

  }

  private TokenType _type;
  private int _offset;
  private String _value;
  private int _line;
  private int _column;

  Token( TokenType type, int offset, String value, int line, int column )
  {
    _type = type;
    _offset = offset;
    _value = value;
    _line = line;
    _column = column;
  }

  public TokenType getType()
  {
    return _type;
  }

  public String getText()
  {
    return _value;
  }

  public int getOffset()
  {
    return _offset;
  }

  public int getLine()
  {
    return _line;
  }

  public int getColumn()
  {
    return _column;
  }
}
