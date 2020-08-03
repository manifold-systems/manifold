/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.preprocessor.expression;

import static manifold.preprocessor.expression.ExpressionTokenType.*;

class ExpressionTokenizer
{
  private CharSequence _buffer;
  private ExpressionTokenType _tokenType;
  private int _bufferIndex;
  private int _bufferEndOffset;
  private int _tokenEndOffset;

  ExpressionTokenizer( CharSequence buffer, int startOffset, int endOffset )
  {
    _buffer = buffer;
    _bufferIndex = startOffset;
    _bufferEndOffset = endOffset;
    _tokenType = null;
    _tokenEndOffset = startOffset;
  }

  ExpressionTokenType getTokenType()
  {
    return _tokenType;
  }

  int getTokenStart()
  {
    return _bufferIndex;
  }

  int getTokenEnd()
  {
    return _tokenEndOffset;
  }

  CharSequence getTokenText()
  {
    if( _tokenType != null )
    {
      return _buffer.subSequence( getTokenStart(), getTokenEnd() );
    }
    return null;
  }

  void advance()
  {
    _tokenType = null;

    if( _tokenEndOffset == _bufferEndOffset )
    {
      _bufferIndex = _bufferEndOffset;
      return;
    }

    _bufferIndex = _tokenEndOffset;

    char c = charAt( _bufferIndex );
    switch( c )
    {
      case ' ':
      case '\t':
        _tokenType = Whitespace;
        _tokenEndOffset = skipWhitespace( _bufferIndex + 1 );
        break;

      case '"':
        _tokenType = StringLiteral;
        _tokenEndOffset = getClosingQuote( _bufferIndex + 1 );
        break;

      case '(':
        _tokenType = OpenParen;
        _tokenEndOffset = _bufferIndex + 1;
        break;
      case ')':
        _tokenType = CloseParen;
        _tokenEndOffset = _bufferIndex + 1;
        break;

      case '|':
        if( _bufferIndex + 1 != _bufferEndOffset &&
            charAt( _bufferIndex + 1 ) == '|' )
        {
          _tokenType = Or;
          _tokenEndOffset = _bufferIndex + 2;
        }
        break;

      case '&':
        if( _bufferIndex + 1 != _bufferEndOffset &&
            charAt( _bufferIndex + 1 ) == '&' )
        {
          _tokenType = And;
          _tokenEndOffset = _bufferIndex + 2;
        }
        break;

      case '=':
        if( _bufferIndex + 1 != _bufferEndOffset &&
            charAt( _bufferIndex + 1 ) == '=' )
        {
          _tokenType = Equals;
          _tokenEndOffset = _bufferIndex + 2;
        }
        break;

      case '>':
        if( _bufferIndex + 1 != _bufferEndOffset &&
            charAt( _bufferIndex + 1 ) == '=' )
        {
          _tokenType = ge;
          _tokenEndOffset = _bufferIndex + 2;
        }
        else
        {
          _tokenType = gt;
          _tokenEndOffset = _bufferIndex + 1;
        }
        break;

      case '<':
        if( _bufferIndex + 1 != _bufferEndOffset &&
            charAt( _bufferIndex + 1 ) == '=' )
        {
          _tokenType = le;
          _tokenEndOffset = _bufferIndex + 2;
        }
        else
        {
          _tokenType = lt;
          _tokenEndOffset = _bufferIndex + 1;
        }
        break;

      case '!':
        if( _bufferIndex + 1 != _bufferEndOffset &&
            charAt( _bufferIndex + 1 ) == '=' )
        {
          _tokenType = NotEquals;
          _tokenEndOffset = _bufferIndex + 2;
        }
        else
        {
          _tokenType = Not;
          _tokenEndOffset = _bufferIndex + 1;
        }
        break;

      default:
        if( Character.isJavaIdentifierStart( c ) )
        {
          _tokenType = Identifier;
          _tokenEndOffset = getIdentifier( c );
        }
    }

    if( _tokenEndOffset > _bufferEndOffset )
    {
      _tokenEndOffset = _bufferEndOffset;
    }
  }

  private int getIdentifier( char c )
  {
    StringBuilder id = new StringBuilder().append( c );
    int offset = _bufferIndex + 1;
    for( ; offset < _bufferEndOffset; offset++ )
    {
      c = charAt( offset );
      if( Character.isJavaIdentifierPart( c ) || c == '.' )
      {
        id.append( c );
      }
      else
      {
        break;
      }
    }
    return offset;
  }

  private char charAt( int i )
  {
    return _buffer.charAt( i );
  }

  private int skipWhitespace( int offset )
  {
    if( offset >= _bufferEndOffset )
    {
      return _bufferEndOffset;
    }

    int pos = offset;
    char c = charAt( pos );

    while( c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' )
    {
      pos++;
      if( pos == _bufferEndOffset )
      {
        return pos;
      }
      c = charAt( pos );
    }

    return pos;
  }

  private int getClosingQuote( int offset )
  {
    if( offset >= _bufferEndOffset )
    {
      return _bufferEndOffset;
    }

    int pos = offset;
    char c = charAt( pos );

    while( true )
    {
      while( c != '"' && c != '\n' && c != '\r' && c != '\\' )
      {
        pos++;
        if( pos >= _bufferEndOffset )
        {
          return _bufferEndOffset;
        }
        c = charAt( pos );
      }

      if( c == '\\' )
      {
        pos++;
        if( pos >= _bufferEndOffset )
        {
          return _bufferEndOffset;
        }
        c = charAt( pos );
        if( c == '\n' || c == '\r' )
        {
          continue;
        }
        pos++;
        if( pos >= _bufferEndOffset )
        {
          return _bufferEndOffset;
        }
        c = charAt( pos );
      }
      else if( c == '"' )
      {
        break;
      }
      else
      {
        pos--;
        break;
      }
    }

    return pos + 1;
  }
}
