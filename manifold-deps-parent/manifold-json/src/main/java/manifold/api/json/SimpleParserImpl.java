package manifold.api.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import manifold.util.Pair;

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
  private final Tokenizer _tokenizer;
  private Token _token;
  private final List<String> _errors;
  private boolean _useBig;
  private boolean _withTokens;

  SimpleParserImpl( Tokenizer tokenizer, boolean useBig )
  {
    _tokenizer = tokenizer;
    _useBig = useBig;
    _errors = new ArrayList<>();
    advance();
  }

  // jsonText = value.
  Object parse()
  {
    return parse( false );
  }
  Object parse( boolean withTokens )
  {
    _withTokens = withTokens;
    Object val = null;
    if( _token.isValueType() )
    {
      val = parseValue();
    }
    else
    {
      addError();
    }
    return val;
  }

  private void advance()
  {
    _token = _tokenizer.next();
  }

  // array = "[" [ value { "," value } ] "]".
  private Object parseArray()
  {
    ArrayList<Object> arr = new ArrayList<>();
    advance();
    if( _token.isValueType() )
    {
      arr.add( parseValue() );
      while( _token.getType() == TokenType.COMMA )
      {
        advance();
        arr.add( parseValue() );
      }
    }
    checkAndSkip( TokenType.RSQUARE, "]" );
    return arr;
  }
  
  // object = "{" [ member { "," member } ] "}".
  private Object parseObject()
  {
    // using a LinkedHashMap to preserve insertion order, necessary for IJ plugin
    Bindings map = new SimpleBindings( new LinkedHashMap<>() );

    advance();
    if( _token.getType() == TokenType.STRING )
    {
      parseMember( map );
      while( _token.getType() == TokenType.COMMA )
      {
        advance();
        parseMember( map );
      }
    }
    checkAndSkip( TokenType.RCURLY, "}" );
    return map;
  }

  // member = string ":" value.
  private void parseMember( Bindings map )
  {
    Token keyToken = _token;
    String key = _token.getString();
    check( TokenType.STRING, "a string" );
    check( TokenType.COLON, ":" );
    Token valueToken = _token;
    Object val = parseValue();
    map.put( key, _withTokens ? new Pair<>( new Token[] {keyToken, valueToken}, val ) : val );
  }

  // value = object | array | number | string | "true" | "false" | "null" .
  private Object parseValue()
  {
    Object val;
    switch( _token.getType() )
    {
      case LCURLY:
        val = parseObject();
        break;
      case LSQUARE:
        val = parseArray();
        break;
      case INTEGER:
        if( _useBig )
        {
          val = new BigInteger( _token.getString() );
        }
        else
        {
          try
          {
            val = Integer.parseInt( _token.getString() );
          }
          catch( NumberFormatException e0 )
          {
            // we have an overflow, the tokenizer guarantees the format is correct
            try
            {
              val = Long.parseLong( _token.getString() );
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
        if( _useBig )
        {
          val = new BigDecimal( _token.getString() );
        }
        else
        {
          val = Double.parseDouble( _token.getString() );
        }
        advance();
        break;
      case STRING:
        val = _token.getString();
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
  
  private void addError()
  {
    _errors.add( "[" + _token.getLineNumber() + ":" + _token.getColumn() + "] Unexpected token '" + _token.getString() + "'" );
    advance();
  }

  private void check( TokenType type, String s )
  {
    if( _token.getType() != type )
    {
      _errors.add( "[" + _token.getLineNumber() + ":" + _token.getColumn() + "] expecting '" + s + "', found '" + _token.getString() + "'" );
    }
    advance();
  }

  private void checkAndSkip( TokenType type, String s )
  {
    if( _token.getType() != type )
    {
      _errors.add( "[" + _token.getLineNumber() + ":" + _token.getColumn() + "] expecting '" + s + "', found '" + _token.getString() + "'" );
      while( _token.getType() != TokenType.EOF &&
             _token.getType() != type )
      {
        advance();
      }
    }
    advance();
  }

  List<String> getErrors()
  {
    return _errors;
  }
}
