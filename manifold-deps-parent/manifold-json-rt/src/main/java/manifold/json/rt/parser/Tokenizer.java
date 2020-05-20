/*
 * Copyright (c) 2020 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.json.rt.parser;

import java.io.IOException;
import java.io.Reader;

final class Tokenizer
{
  private Reader _source;
  private char _ch;
  private int _line;
  private int _column;
  private int _offset;

  Tokenizer( Reader source )
  {
    _source = source;
    _offset = 0;
    _line = 1;
    _column = 0;
    nextChar();
  }

  Token next()
  {
    Token T;
    eatWhiteSpace();
    switch( _ch )
    {
      case '"':
      case '\'':
        T = consumeString( _ch );
        break;
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        T = consumeNumber();
        break;
      case '{':
        T = new Token( TokenType.LCURLY, "{", _offset, _line, _column );
        nextChar();
        break;
      case '}':
        T = new Token( TokenType.RCURLY, "}", _offset, _line, _column );
        nextChar();
        break;
      case '[':
        T = new Token( TokenType.LSQUARE, "[", _offset, _line, _column );
        nextChar();
        break;
      case ']':
        T = new Token( TokenType.RSQUARE, "]", _offset, _line, _column );
        nextChar();
        break;
      case ',':
        T = new Token( TokenType.COMMA, ",", _offset, _line, _column );
        nextChar();
        break;
      case ':':
        T = new Token( TokenType.COLON, ":", _offset, _line, _column );
        nextChar();
        break;
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
      case 'g':
      case 'h':
      case 'i':
      case 'j':
      case 'k':
      case 'l':
      case 'm':
      case 'n':
      case 'o':
      case 'p':
      case 'q':
      case 'r':
      case 's':
      case 't':
      case 'u':
      case 'v':
      case 'w':
      case 'x':
      case 'y':
      case 'z':
      case 'A':
      case 'B':
      case 'C':
      case 'D':
      case 'E':
      case 'F':
      case 'G':
      case 'H':
      case 'I':
      case 'J':
      case 'K':
      case 'L':
      case 'M':
      case 'N':
      case 'O':
      case 'P':
      case 'Q':
      case 'R':
      case 'S':
      case 'T':
      case 'U':
      case 'V':
      case 'W':
      case 'X':
      case 'Y':
      case 'Z':
        T = consumeConstant();
        break;
      case '\0':
        T = new Token( TokenType.EOF, "EOF", _offset, _line, _column );
        break;
      default:
        T = new Token( TokenType.ERROR, String.valueOf( _ch ), _offset, _line, _column );
        nextChar();
    }
    return T;
  }

  /*
    string = '"' {char} '"' | "'" {char} "'".
    char = unescaped | "\" ('"' | "\" | "/" | "b" | "f" | "n" | "r" | "t" | "u" hex hex hex hex).
    unescaped = any printable Unicode character except '"', "'" or "\".
  */
  private Token consumeString( char quote )
  {
    StringBuilder sb = new StringBuilder();
    int line = _line;
    int column = _column;
    int offset = _offset;
    Token T;
    nextChar();
    while( moreChars() && _ch != quote )
    {
      if( _ch == '\\' )
      {
        nextChar();
        switch( _ch )
        {
          case '"':
          case '\\':
          case '/':
            sb.append( _ch );
            nextChar();
            break;
          case 'b':
            sb.append( '\b' );
            nextChar();
            break;
          case 'f':
            sb.append( '\f' );
            nextChar();
            break;
          case 'n':
            sb.append( '\n' );
            nextChar();
            break;
          case 'r':
            sb.append( '\r' );
            nextChar();
            break;
          case 't':
            sb.append( '\t' );
            nextChar();
            break;
          case 'u':
            nextChar();
            int u = 0;
            for( int i = 0; i < 4; i++ )
            {
              if( isHexDigit( _ch ) )
              {
                u = u * 16 + _ch - '0';
                if( _ch >= 'A' )
                { // handle hex numbers: 'A' = 65, '0' = 48. 'A'-'0' = 17, 17 - 7 = 10
                  u = u - 7;
                }
              }
              else
              {
                T = new Token( TokenType.ERROR, sb.toString(), _offset, _line, _column );
                nextChar();
                return T;
              }
              nextChar();
            }
            sb.append( (char)u );
            break;
          default:
            T = new Token( TokenType.ERROR, sb.toString(), _offset, _line, _column );
            nextChar();
            return T;
        }
      }
      else
      {
        sb.append( _ch );
        nextChar();
      }
    }
    if( _ch == quote )
    {
      T = new Token( TokenType.STRING, sb.toString(), offset, line, column );
    }
    else
    {
      T = new Token( TokenType.ERROR, sb.toString(), _offset, _line, _column );
    }
    nextChar();
    return T;
  }

  /*
    number = [ "-" ] int [ frac ] [ exp ].
    exp = ("e" | "E") [ "-" | "+" ] digit {digit}.
    frac = "." digit {digit}.
    int = "0" |  digit19 {digit}.
    digit = "0" | "1" | ... | "9".
    digit19 = "1" | ... | "9".
  */
  private Token consumeNumber()
  {
    StringBuilder sb = new StringBuilder();
    int line = _line;
    int column = _column;
    int offset = _offset;
    Token T;
    boolean err;
    boolean isDouble = false;
    if( _ch == '-' )
    {
      sb.append( _ch );
      nextChar();
    }
    if( _ch != '0' )
    {
      err = consumeDigits( sb );
      if( err )
      {
        return new Token( TokenType.ERROR, sb.toString(), _offset, _line, _column );
      }
    }
    else
    {
      sb.append( _ch );
      nextChar();
    }
    if( _ch == '.' )
    {
      isDouble = true;
      sb.append( _ch );
      nextChar();
      err = consumeDigits( sb );
      if( err )
      {
        return new Token( TokenType.ERROR, sb.toString(), _offset, _line, _column );
      }
    }
    if( _ch == 'E' || _ch == 'e' )
    {
      isDouble = true;
      sb.append( _ch );
      nextChar();
      if( _ch == '-' )
      {
        sb.append( _ch );
        nextChar();
      }
      else if( _ch == '+' )
      {
        sb.append( _ch );
        nextChar();
      }
      err = consumeDigits( sb );
      if( err )
      {
        return new Token( TokenType.ERROR, sb.toString(), _offset, _line, _column );
      }
    }
    if( isDouble )
    {
      T = new Token( TokenType.DOUBLE, sb.toString(), offset, line, column );
    }
    else
    {
      T = new Token( TokenType.INTEGER, sb.toString(), offset, line, column );
    }
    return T;
  }

  private boolean consumeDigits( StringBuilder sb )
  {
    boolean err = false;
    if( isDigit( _ch ) )
    {
      while( moreChars() && isDigit( _ch ) )
      {
        sb.append( _ch );
        nextChar();
      }
    }
    else
    {
      err = true;
    }
    return err;
  }

  private boolean isDigit( char ch )
  {
    return ch >= '0' && ch <= '9';
  }

  private boolean isHexDigit( char ch )
  {
    return ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f';
  }

  private Token consumeConstant()
  {
    StringBuilder sb = new StringBuilder();
    Token T;
    int l = _line;
    int c = _column;
    do
    {
      sb.append( _ch );
      nextChar();
    } while( moreChars() && (_ch >= 'a' && _ch <= 'z' || _ch >= 'A' && _ch <= 'Z') );
    String str = sb.toString();
    TokenType type = Token.Constants.get( str );
    if( type == null )
    {
      T = new Token( TokenType.ERROR, str, _offset, l, c );
    }
    else
    {
      T = new Token( type, str, _offset, l, c );
    }
    return T;
  }

  private void eatWhiteSpace()
  {
    while( moreChars() && (_ch == '\t' || _ch == '\n' || _ch == '\r' || _ch == ' ') )
    {
      nextChar();
    }
  }

  private void nextChar()
  {
    int c;

    try
    {
      c = _source.read();
      _offset++;
    }
    catch( IOException e )
    {
      c = -1;
    }
    if( c == '\n' )
    {
      _column = 0;
      _line++;
    }
    else if( c != -1 )
    {
      _column++;
    }
    else
    {
      c = '\0';
    }
    _ch = (char)c;
  }

  private boolean moreChars()
  {
    return _ch != '\0';
  }
}
