package manifold.js.parser.tree;

import manifold.js.parser.Tokenizer;

/**
 */
public class ParseError
{
  private String _message;
  private Tokenizer.Token _token;

  public ParseError( String message, Tokenizer.Token token )
  {
    _message = message;
    _token = token;
  }

  public String getMessage()
  {
    return _message;
  }

  public Tokenizer.Token getToken()
  {
    return _token;
  }
}
