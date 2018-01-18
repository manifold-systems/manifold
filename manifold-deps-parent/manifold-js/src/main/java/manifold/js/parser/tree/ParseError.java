package manifold.js.parser.tree;

import manifold.js.parser.Token;

/**
 */
public class ParseError
{
  private String _message;
  private Token _token;

  public ParseError( String message, Token token )
  {
    _message = message;
    _token = token;
  }

  public String getMessage()
  {
    return _message;
  }

  public Token getToken()
  {
    return _token;
  }
}
