/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.js.rt.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


public class Tokenizer
{
  private URL _url;
  private int _bLineNumber, _bCol, _bOffset; //Keeps track of beginning position of tokens
  private int _lineNumber, _col, _offset; //Keeps track of current position of tokenizer
  private String _content;
  private char _ch;

  public Tokenizer( String source, String url )
  {
    init( source, url );
  }

  private void init( String content, String url )
  {
    _content = content.replace( "\r\n", "\n" );
    //Line number and col are 1 indexed; offset is 0 indexed (nextchar increments col and offset)
    _lineNumber = 1;
    _col = 0;
    _offset = -1;
    try
    {
      _url = new URL( url );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
    nextChar();
  }

  URL getUrl()
  {
    return _url;
  }

  Token nextNonWhiteSpace()
  {
    Token tok = next();
    while( tok.getType() == TokenType.WHITESPACE )
    {
      tok = next();
    }
    return tok;
  }

  public Token next()
  {
    updatePosition(); //keep track of position when we begin consuming a token
    Token ret;

    if( Character.isWhitespace( _ch ) )
    {
      ret = consumeWhitespace();
    }
    else if( _ch == '\'' || _ch == '"' )
    {
      ret = consumeString();
    }
    else if( _ch == '`' )
    {
      ret = consumeTemplateString();
    }
    else if( TokenType.startsIdentifier( _ch ) )
    {
      ret = consumeWord();
    }
    else if( _ch == '.' )
    {
      //For numbers that start with the decimal point
      if( TokenType.isDigit( peek() ) )
      {
        ret = consumeNumber();
      }
      else
      {
        ret = consumePunctuation();
      }
    }
    else if( TokenType.isDigit( _ch ) )
    {
      ret = consumeNumber();
    }
    else if( TokenType.isPunctuation( _ch ) )
    {
      ret = consumePunctuation();
    }
    else if( _ch == '/' )
    {
      /*Forward slash will either result in a single line comment, multiline comment, or a operator token*/
      switch( peek() )
      {
        case '*':
          ret = consumeMultilineComment();
          break;
        case '/':
          ret = consumeSingleLineComment();
          break;
        default:
          ret = consumeOperator();
      }
    }
    else if( TokenType.isPartOfOperator( _ch ) )
    {
      ret = consumeOperator();
    }
    else if( reachedEOF() )
    {
      ret = newToken( TokenType.EOF, "EOF" );
    }
    else
    {
      ret = errToken( String.valueOf( _ch ), "unknown char" );
      nextChar();
    }
    if( ret.getType() == TokenType.COMMENT )
    {
      return next(); //Throw away comments
    }
    return ret;
  }


  /*Possible numbers are integers (decimal, hex, octal or binary)
    and floating point numbers (can have decimal point and exponent);
    Rules taken from mozilla docs Grammar and Types
    */
  private Token consumeNumber()
  {
    boolean isDec = true, isBin = false, isHex = false, isOctal = false, isImpliedOctal = false;
    boolean hasDecPoint = false;
    StringBuilder val = new StringBuilder();
    if( _ch == '0' )
    {
      isDec = false;
      //Mark if explicitly hex, octal, or binary
      if( "oOxXbB".indexOf( peek() ) >= 0 )
      {
        val.append( _ch );
        nextChar();
        if( _ch == 'b' || _ch == 'B' )
        {
          isBin = true;
        }
        else if( _ch == 'x' || _ch == 'X' )
        {
          isHex = true;
        }
        else if( _ch == 'o' || _ch == 'O' )
        {
          isOctal = true;
        }
        val.append( _ch );
        nextChar();
      }
      else
      {
        //Octal is implied if number starts with 0, but can still be dec if a 8 or 9 follows
        isImpliedOctal = true;
      }
    }
    while( !(isDec && hasDecPoint && _ch == '.') && //Limit one decimal point to floating point num
           (isDec && (TokenType.isDigit( _ch ) || _ch == '.') || //Only dec can have decimal points
            (isHex && TokenType.isHexCh( _ch )) ||
            ((isOctal || isImpliedOctal) && TokenType.isOctal( _ch ))) ||
           (isBin && TokenType.isBinary( _ch )) )
    {
      if( _ch == '.' )
      {
        hasDecPoint = true;
      }
      val.append( _ch );
      nextChar();
      if( isDec && (_ch == 'e' || _ch == 'E') )
      {
        return consumeExponent( val );
      }
      //changes octal to dec, so 0777 will be octal and 0778 will be dec
      if( isImpliedOctal && (_ch == '8' || _ch == '9') )
      {
        isDec = true;
        isImpliedOctal = false;
      }
    }
    //If explicitly starts with 0x, 0o, or 0b; throw an error if nothing after
    if( (isHex || isBin || isOctal) && val.length() <= 2 )
    {
      return errToken( val.toString(), "illegal number token" );
    }
    return newToken( TokenType.NUMBER, val.toString() );
  }

  /* Helper for consume number; Consumes the exponent segment of a number, which will start with e (or E),
   * is optionally followed by a sign, and then contains only integers
   */
  private Token consumeExponent( StringBuilder val )
  {
    val.append( _ch ); //consume 'e' or 'E'
    nextChar();
    //Consume optional + or -
    if( _ch == '+' || _ch == '-' )
    {
      val.append( _ch );
      nextChar();
    }
    for( ; TokenType.isDigit( _ch ); nextChar() )
    {
      val.append( _ch );
    }
    return newToken( TokenType.NUMBER, val.toString() );
  }


  /* Consumes and returns an word (either a identifier, keyword, boolean literal, or null)
   * Rules for identifier names from emca-262 11.6.1
   */
  private Token consumeWord()
  {
    StringBuilder val = new StringBuilder();
    for( ; TokenType.partOfIdentifier( _ch ); nextChar() )
    {
      val.append( _ch );
    }
    String strVal = val.toString();
    if( TokenType.isKeyword( strVal ) )
    {
      return newToken( TokenType.KEYWORD, strVal );
    }
    else if( TokenType.isNull( strVal ) )
    {
      return newToken( TokenType.NULL, strVal );
    }
    else if( TokenType.isBoolean( strVal ) )
    {
      return newToken( TokenType.BOOLEAN, strVal );
    }
    else if( TokenType.isClass( strVal ) )
    {
      return newToken( TokenType.CLASS, strVal );
    }
    else
    {
      return newToken( TokenType.IDENTIFIER, strVal );
    }
  }

  /* Syntax for string literal taken from emca 6 language specs 11.8.4
   * Since tokenizer is for transpiler, do not worry about escaping characters
   */
  private Token consumeString()
  {
    char enterQuote = _ch; //Can be either ' or "
    String errorMsg = null;
    StringBuilder val = new StringBuilder( String.valueOf( _ch ) );
    nextChar();
    //Consume string until we find a non-escaped quote matching the enter quote
    while( !(_ch == enterQuote) )
    {
      //error if EOF comes before terminating quote
      if( reachedEOF() )
      {
        return errToken( val.toString(), "unterminated string" );
      }
      //error if line terminator in string
      if( TokenType.isLineTerminator( _ch ) )
      {
        errorMsg = "newline character in string";
      }

      val.append( _ch );
      //Make sure escape sequences are legal
      if( _ch == '\\' )
      {
        errorMsg = consumeEscapeSequence( val );
      }
      else
      {
        nextChar();
      }
    }
    val.append( _ch ); //add closing quote
    nextChar();
    if( errorMsg != null )
    {
      return errToken( val.toString(), errorMsg );
    }
    return newToken( TokenType.STRING, val.toString() );
  }

  /* Consume template string as a entire raw string. Template tokenizing and parsing are handled
    in the parser
   */
  private Token consumeTemplateString()
  {
    nextChar(); //skip over `
    StringBuilder val = new StringBuilder();
    while( _ch != '`' )
    {
      if( reachedEOF() )
      {
        return errToken( val.toString(), "unterminated string template" );
      }
      val.append( _ch );
      nextChar();
    }
    nextChar();//skip over closing backtick
    return newToken( TokenType.TEMPLATESTRING, val.toString() );
  }

  /*Helper to consume and validate escape sequences;
   *  Marks invalid unicode and hex escapes are illegal; octal escapes should always be legal.
   *  Returns an error message if illegal escape */
  private String consumeEscapeSequence( StringBuilder val )
  {
    nextChar();
    switch( _ch )
    {
      case 'u':
        return consumeUnicodeEscapeSequence( val );
      case 'x':
        return consumeHexEscapeSequence( val );
      /* Consumes single escapes (' " \ b \f n r t v), and non-escaped (such as 'a' where \ will be ignored)
       * and newlines and ending quotes (for line continuation)
       */
      default:
        val.append( _ch );
        nextChar();
        return null;
    }
  }

  /*Unicode escape sequence are either uHexHexHexHex or u{Hex+}*/
  private String consumeUnicodeEscapeSequence( StringBuilder val )
  {
    final long MAX_UNICODE_NUM = 0x10FFFF;
    val.append( _ch ); //consume 'u'
    nextChar();
    if( _ch == '{' )
    {
      val.append( _ch );
      nextChar();
      StringBuilder num = new StringBuilder(); //keep track of hex number to check if valid unicode
      for( ; _ch != '}'; nextChar() )
      {
        if( !TokenType.isHexCh( _ch ) )
        {
          return "non-hex character in unicode escape";
        }
        num.append( _ch );
        val.append( _ch );
      }
      val.append( _ch ); //consume closing }
      nextChar();
      //error if exceeds max unicode number
      if( Long.parseLong( num.toString(), 16 ) > MAX_UNICODE_NUM )
      {
        return "undefined Unicode point";
      }
      else
      {
        return null;
      }
    }
    else
    {
      //Must have exactly 4 hex digits in this pattern
      for( int i = 0; i < 4; i++ )
      {
        if( !TokenType.isHexCh( _ch ) )
        {
          return "non-hex character in unicode escape";
        }
        val.append( _ch );
        nextChar();
      }
      return null;
    }
  }

  /*hex escape sequences must be uHexHex*/
  private String consumeHexEscapeSequence( StringBuilder val )
  {
    val.append( _ch ); //consume 'x'
    nextChar();
    for( int i = 0; i < 2; i++ )
    {
      if( !TokenType.isHexCh( _ch ) )
      {
        return "non-hex character in hex escape";
      }
      val.append( _ch );
      nextChar();
    }
    return null;
  }


  /*Consumes punctuation, which are all single characters*/
  private Token consumePunctuation()
  {
    Token tok = newToken( TokenType.PUNCTUATION, String.valueOf( _ch ) );
    nextChar();
    return tok;
  }

  private Token consumeOperator()
  {
    StringBuilder val = new StringBuilder();
    /*Keep consuming until we reach a non operator character or when adding the character makes a
     non-valid operator, since every multi-character operator is built off a shorter operator
      */
    for( ; TokenType.isPartOfOperator( _ch ) && TokenType.isOperator( val.toString() + _ch ); nextChar() )
    {
      val.append( _ch );
    }
    return newToken( TokenType.OPERATOR, val.toString() );
  }

  private Token consumeMultilineComment()
  {
    StringBuilder val = new StringBuilder( "/*" );
    nextChar();
    nextChar(); //Consume first two chars, which we know make '/*'
    for( ; !(_ch == '/' && val.charAt( val.length() - 1 ) == '*'); nextChar() )
    {
      val.append( _ch );
      //error if EOF comes before terminating quote
      if( reachedEOF() )
      {
        return newToken( TokenType.ERROR, "unterminated multiline comment" );
      }
    }
    val.append( _ch ); //append closing slash
    nextChar();
    return newToken( TokenType.COMMENT, val.toString() );
  }

  private Token consumeSingleLineComment()
  {
    StringBuilder val = new StringBuilder( "//" );
    nextChar();
    nextChar(); //Consume first two chars, which we know make '//'
    for( ; !(_ch == '\n' || _ch == '\r' || reachedEOF()); nextChar() )
    {
      val.append( _ch );
    }
    return newToken( TokenType.COMMENT, val.toString() );
  }

  private Token consumeWhitespace()
  {
    StringBuilder val = new StringBuilder();
    while( Character.isWhitespace( _ch ) )
    {
      val.append( _ch );
      nextChar();
    }
    return newToken( TokenType.WHITESPACE, val.toString() );
  }


  //========================================================================================
  // Utilities
  //========================================================================================

  //Returns the next character in the stream without updating _ch
  char peek()
  {
    int next = 1 + _offset;
    return next >= _content.length() ? '\0' : _content.charAt( next );
  }

  void nextChar()
  {
    if( _offset + 1 >= _content.length() )
    {
      _offset = _content.length();
      _ch = (char)-1;
      return;
    }

    _ch = _content.charAt( ++_offset );
    _col++;
    if( _ch == '\n' )
    {
      _col = 0;
      _lineNumber++;
    }
  }

  //Updates the start token position when consuming a new token
  private void updatePosition()
  {
    _bCol = _col;
    _bLineNumber = _lineNumber;
    _bOffset = _offset;
  }


  char currChar()
  {
    return _ch;
  }

  Token newToken( TokenType type, String val )
  {
    return new Token( type, val, _bLineNumber, _bCol, _bOffset );
  }

  private Token errToken( String val, String errorMsg )
  {
    return new Token( TokenType.ERROR, val, errorMsg );
  }


  boolean reachedEOF()
  {
    return _ch == (char)-1;
  }

  public List<Token> tokenize()
  {
    List<Token> tokens = new LinkedList<Token>();
    for( Token token = next(); token.getType() != TokenType.EOF; token = next() )
    {
      tokens.add( token );
    }
    return tokens;
  }

}
