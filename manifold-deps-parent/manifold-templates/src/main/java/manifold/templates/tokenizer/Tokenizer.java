package manifold.templates.tokenizer;

import java.util.ArrayList;
import java.util.List;


import static manifold.templates.tokenizer.Token.TokenType.*;

public class Tokenizer
{
  private int _index;
  private List<Token> _tokens;
  private CharSequence _text;
  private StringBuilder _stuff;

  private int _tokenIndex;
  private int _stuffLine;
  private int _stuffColumn;
  private boolean _isParsingString;
  private boolean _isParsingCharLiteral;

  public Tokenizer()
  {
  }

  public List<Token> tokenize( CharSequence text )
  {
    _tokens = new ArrayList<>();
    _tokenIndex = -1;
    _index = 0;
    _text = text;
    _isParsingString = false;
    _isParsingCharLiteral = false;
    _stuff = new StringBuilder();

    int line = 1;
    int column = 1;
    int index = _index;
    boolean escaped = false;

    nextToken();

    while( true )
    {
      if( index >= _text.length() )
      {
        break;
      }

      int before = index;
      char c = _text.charAt( index );

      if( c == '\n' )
      {
        line++;
        column = 1;
      }

      if( !escaped && c == '\\' && !isInCode() && _text.length() > index+1 &&
          (_text.charAt( index+1 ) == '<' || _text.charAt( index+1 ) == '$') )
      {
        escaped = true;
        index++;
        continue;
      }

      if( isTop( COMMENT_BEGIN ) )
      {
        if( c == '-' )
        {
          index++;
          if( charIs( index, '-' ) )
          {
            index++;
            if( charIs( index, '%' ) )
            {
              index++;
              if( charIs( index, '>' ) )
              {
                pushStuff();
                pushToken( COMMENT_END, ++index, line, column );
                continue;
              }
            }
          }
        }
      }
      else
      {
        if( c == '$' && !isInCode() && !isParsingString() && !escaped )
        {
          index++;
          if( charIs( index, '{' ) )
          {
            pushStuff();
            pushToken( EXPR_BRACE_BEGIN, ++index, line, column );
            continue;
          }
        }
        else if( c == '<' && !isInCode() && !escaped )
        {
          index++;
          if( charIs( index, '%' ) )
          {
            pushStuff();
            index++;
            if( _text.charAt( index ) == '=' )
            {
              pushToken( EXPR_ANGLE_BEGIN, ++index, line, column );
              continue;
            }
            else if( charIs( index, '@' ) )
            {
              pushToken( DIR_ANGLE_BEGIN, ++index, line, column );
              continue;
            }
            else if( charIs( index, '-' ) )
            {
              if( charIs( index + 1, '-' ) )
              {
                pushToken( COMMENT_BEGIN, index += 2, line, column );
                continue;
              }
            }

            pushToken( STMT_ANGLE_BEGIN, index, line, column );
            continue;
          }
        }
        else if( c == '}' && isTop( EXPR_BRACE_BEGIN ) && isInCode() && !isParsingString() && !isParsingCharLiteral() )
        {
          pushStuff();
          pushToken( EXPR_BRACE_END, ++index, line, column );
          continue;
        }
        else if( c == '%' && isInCode() && !isParsingString() )
        {
          index++;
          if( charIs( index, '>' ) )
          {
            pushStuff();
            pushToken( ANGLE_END, ++index, line, column );
            continue;
          }
        }
      }
      if( _stuff.length() == 0 )
      {
        _stuffLine = line;
        _stuffColumn = column;
      }
      _stuff.append( c );
      index = before + 1;
      setParsingString( c, before );
      setParsingCharLiteral( c, before );
      escaped = false;
    }

    pushStuff();

    try
    {
      return _tokens;
    }
    finally
    {
      clear();
    }
  }

  private void nextToken()
  {
    if( _tokenIndex + 1 < _tokens.size() )
    {
      _tokenIndex++;
    }
    else
    {
      _tokenIndex = -1;
    }
  }

  private boolean charIs( int index, char c )
  {
    return _text.length() > index && _text.charAt( index ) == c;
  }

  private void pushStuff()
  {
    if( _stuff == null || _stuff.length() == 0 )
    {
      return;
    }

    Token.TokenType beginType = _tokens.size() == 0 ? null : peek().getType();
    Token.TokenType stuffType;
    if( beginType == EXPR_BRACE_BEGIN ||
        beginType == EXPR_ANGLE_BEGIN )
    {
      stuffType = EXPR;
    }
    else if( beginType == STMT_ANGLE_BEGIN )
    {
      stuffType = STMT;
    }
    else if( beginType == DIR_ANGLE_BEGIN )
    {
      stuffType = DIRECTIVE;
    }
    else if( beginType == COMMENT_BEGIN )
    {
      stuffType = COMMENT;
    }
    else
    {
      stuffType = CONTENT;
    }
    _tokens.add( new Token( stuffType, _index, _stuff.toString(), _stuffLine, _stuffColumn ) );
    _index += _stuff.length();
    _stuff = new StringBuilder();
  }

  private void pushToken( Token.TokenType tokenType, int index, int line, int column )
  {
    String token = tokenType.getToken();
    if( token == null )
    {
      throw new IllegalStateException( "Expected static token, but found: " + tokenType.name() );
    }
    _tokens.add( new Token( tokenType, _index, token, line, column ) );
    _index = index;
  }

  private boolean isInCode()
  {
    return isTop( EXPR_ANGLE_BEGIN ) ||
           isTop( EXPR_BRACE_BEGIN ) ||
           isTop( STMT_ANGLE_BEGIN ) ||
           isTop( DIR_ANGLE_BEGIN ) ||
           isTop( COMMENT_BEGIN );
  }

  private boolean isTop( Token.TokenType tokenType )
  {
    return !_tokens.isEmpty() && peek().getType() == tokenType;
  }

  private void setParsingString( char c, int index )
  {
    if( !isInCode() || c != '"' )
    {
      return;
    }

    if( !isParsingString() )
    {
      _isParsingString = true;
    }
    else if( _text.charAt( index -1 ) != '\\' )
    {
      _isParsingString = false;
    }
  }

  private boolean isParsingString()
  {
    return _isParsingString;
  }

  private void setParsingCharLiteral( char c, int index )
  {
    if( !isInCode() || c != '\'' || isParsingString() )
    {
      return;
    }

    if( !isParsingCharLiteral() )
    {
      _isParsingCharLiteral = true;
    }
    else if( _text.charAt( index -1 ) != '\\' )
    {
      _isParsingCharLiteral = false;
    }
  }

  private boolean isParsingCharLiteral()
  {
    return _isParsingCharLiteral;
  }

  private Token peek()
  {
    return _tokens.get( _tokens.size()-1 );
  }

  private void clear()
  {
    _text = null;
    _tokens = null;
    _stuff = null;
    _index = -1;
  }
}

