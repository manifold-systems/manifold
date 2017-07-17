package manifold.api.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.SimpleBindings;

/*
  http://tools.ietf.org/html/rfc7159

  jsonText = value.
  object = "{" [ member { "," member } ] "}".
  member = string ":" value.
  array = "[" [ value { "," value } ] "]".
  value = object | array | number | string | "true" | "false" | "null" .
  number = [ "-" ] int [ frac ] [ exp ].
  exp = ("e" | "E") [ "-" | "+" ] digit {digit}.
  frac = "." digit {digit}.
  int = "0" |  digit19 {digit}.
  digit = "0" | "1" | ... | "9".
  hex = digit | "A" | ... | "F" | "a" | ... | "F".
  digit19 = "1" | ... | "9".
  string = '"' {char} '"' | "'" {char} "'".
  char = unescaped | "\" ('"' | "\" | "/" | "b" | "f" | "n" | "r" | "t" | "u" hex hex hex hex).
  unescaped = any printable Unicode character except '"', "'" or "\".
  ws =  { " " | "\t" | "\n" | "\r" }.
 */

final class SimpleParserImpl
{
  private final Tokenizer tokenizer;
  private Token T;
  private final List<String> errors;
  private boolean useBig;

  public SimpleParserImpl( Tokenizer tokenizer, boolean useBig )
  {
    this.tokenizer = tokenizer;
    this.useBig = useBig;
    errors = new ArrayList<String>();
    advance();
  }

  public void advance()
  {
    T = tokenizer.next();
  }

  public boolean hasMore()
  {
    return T.getType() != TokenType.EOF;
  }

  public Token currentToken()
  {
    return T;
  }

  // jsonText = value.
  public Object parse()
  {
    Object val = null;
    if( T.isValueType() )
    {
      val = parseValue();
    }
    else
    {
      addError();
    }
    return val;
  }

  // array = "[" [ value { "," value } ] "]".
  public Object parseArray()
  {
    ArrayList arr = new ArrayList();
    advance();
    if( T.isValueType() )
    {
      arr.add( parseValue() );
      while( T.getType() == TokenType.COMMA )
      {
        advance();
        arr.add( parseValue() );
      }
    }
    checkAndSkip( TokenType.RSQUARE, "]" );
    return arr;
  }

  public void skipArray()
  {
    advance();
    if( T.isValueType() )
    {
      skipValue();
      while( T.getType() == TokenType.COMMA )
      {
        advance();
        skipValue();
      }
    }
    checkAndSkip( TokenType.RSQUARE, "]" );
  }

  // object = "{" [ member { "," member } ] "}".
  public Object parseObject()
  {
    Map map = new SimpleBindings();
    advance();
    if( T.getType() == TokenType.STRING )
    {
      parseMember( map );
      while( T.getType() == TokenType.COMMA )
      {
        advance();
        parseMember( map );
      }
    }
    checkAndSkip( TokenType.RCURLY, "}" );
    return map;
  }

  public void skipObject()
  {
    advance();
    if( T.getType() == TokenType.STRING )
    {
      skipMember();
      while( T.getType() == TokenType.COMMA )
      {
        advance();
        skipMember();
      }
    }
    checkAndSkip( TokenType.RCURLY, "}" );
  }

  // member = string ":" value.
  public void parseMember( Map map )
  {
    String key = T.getString();
    check( TokenType.STRING, "a string" );
    check( TokenType.COLON, ":" );
    Object val = parseValue();
    map.put( key, val );
  }

  public void skipMember()
  {
    check( TokenType.STRING, "a string" );
    check( TokenType.COLON, ":" );
    skipValue();
  }

  // value = object | array | number | string | "true" | "false" | "null" .
  public Object parseValue()
  {
    Object val;
    switch( T.getType() )
    {
      case LCURLY:
        val = parseObject();
        break;
      case LSQUARE:
        val = parseArray();
        break;
      case INTEGER:
        if( useBig )
        {
          val = new BigInteger( T.getString() );
        }
        else
        {
          try
          {
            val = Integer.parseInt( T.getString() );
          }
          catch( NumberFormatException e0 )
          {
            // we have an overflow, the tokenizer guarantees the format is correct
            try
            {
              val = Long.parseLong( T.getString() );
            }
            catch( NumberFormatException e1 )
            {
              val = 0;
            }
          }
        }
        advance();
        break;
      case DOUBLE:
        if( useBig )
        {
          val = new BigDecimal( T.getString() );
        }
        else
        {
          val = Double.parseDouble( T.getString() );
        }
        advance();
        break;
      case STRING:
        val = T.getString();
        advance();
        break;
      case TRUE:
        val = true;
        advance();
        break;
      case FALSE:
        val = false;
        advance();
        break;
      case NULL:
        val = null;
        advance();
        break;
      default:
        val = null;
        addError();
    }
    return val;
  }

  public void skipValue()
  {
    switch( T.getType() )
    {
      case LCURLY:
        skipObject();
        break;
      case LSQUARE:
        skipArray();
        break;
      case INTEGER:
      case DOUBLE:
      case STRING:
      case TRUE:
      case FALSE:
      case NULL:
        advance();
        break;
      default:
        addError();
    }
  }

  private void addError()
  {
    errors.add( "[" + T.getLineNumber() + ":" + T.getColumn() + "] Unexpected token '" + T.getString() + "'" );
    advance();
  }

  private void check( TokenType type, String s )
  {
    if( T.getType() != type )
    {
      errors.add( "[" + T.getLineNumber() + ":" + T.getColumn() + "] expecting '" + s + "', found '" + T.getString() + "'" );
    }
    advance();
  }

  private void checkAndSkip( TokenType type, String s )
  {
    if( T.getType() != type )
    {
      errors.add( "[" + T.getLineNumber() + ":" + T.getColumn() + "] expecting '" + s + "', found '" + T.getString() + "'" );
      while( T.getType() != TokenType.EOF &&
             T.getType() != type )
      {
        advance();
      }
    }
    advance();
  }

  public List<String> getErrors()
  {
    return errors;
  }
}
