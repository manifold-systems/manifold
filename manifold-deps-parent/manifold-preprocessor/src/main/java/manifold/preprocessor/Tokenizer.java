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

package manifold.preprocessor;

import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.util.function.Consumer;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.expression.Expression;
import manifold.preprocessor.expression.ExpressionParser;

public class Tokenizer
{
  private CharSequence _buffer;
  private TokenType _tokenType;
  private int _bufferIndex;
  private int _bufferEndOffset;
  private int _tokenEndOffset;
  private Expression _expr;
  private final Consumer<Tokenizer> _consumer;

  public Tokenizer( CharSequence buffer, int startOffset, int endOffset, Consumer<Tokenizer> consumer )
  {
    _buffer = buffer;
    _bufferIndex = startOffset;
    _bufferEndOffset = endOffset;
    _tokenType = null;
    _tokenEndOffset = startOffset;
    _consumer = consumer;
  }

  public TokenType getTokenType()
  {
    return _tokenType;
  }

  public int getTokenStart()
  {
    return _bufferIndex;
  }

  public int getTokenEnd()
  {
    return _tokenEndOffset;
  }

  public Expression getExpression()
  {
    return _expr;
  }

  public void advance()
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
      case '\n':
      case '\r':
      case '\f':
        _tokenType = TokenType.Whitespace;
        _tokenEndOffset = skipWhitespace( _bufferIndex + 1, true );
        break;

      case '/':
        if( _bufferIndex + 1 < _bufferEndOffset )
        {
          char nextChar = charAt( _bufferIndex + 1 );
          if( nextChar == '/' )
          {
            _tokenType = TokenType.LineComment;
            _tokenEndOffset = getLineTerminator( _bufferIndex + 2 );
          }
          else if( nextChar == '*' )
          {
            _tokenType = TokenType.BlockComment;
            _tokenEndOffset = getClosingComment( _bufferIndex + 2 );
          }
          else
          {
            makeSourceCodeToken( _bufferIndex + 1 );
          }
        }
        break;

      case '"':
        if( _bufferIndex + 2 < _bufferEndOffset && charAt( _bufferIndex + 2 ) == '"' && charAt( _bufferIndex + 1 ) == '"' )
        {
          _tokenType = TokenType.TextBlock;
          _tokenEndOffset = getTextBlockEnd( _bufferIndex + 2 );
        }
        else
        {
          _tokenType = TokenType.StringLiteral;
          _tokenEndOffset = getClosingQuote( _bufferIndex + 1, c );
        }
        break;

      case '\'':
        _tokenType = TokenType.CharLiteral;
        _tokenEndOffset = getClosingQuote( _bufferIndex + 1, c );
        break;

      case '#':
        if( !matchDirective( _bufferIndex + 1 ) )
        {
          addError( "Invalid directive", _bufferIndex );
          makeSourceCodeToken( _bufferIndex + 1 );
        }
        break;

      default:
        makeSourceCodeToken( _bufferIndex + 1 );
    }

    if( _tokenEndOffset > _bufferEndOffset )
    {
      _tokenEndOffset = _bufferEndOffset;
    }

    if( _consumer != null )
    {
      _consumer.accept( this );
    }
  }

  private boolean matchDirective( int offset )
  {
    if( match( TokenType.If.getDirective(), offset ) )
    {
      offset += TokenType.If.getDirective().length();
      _tokenType = TokenType.If;

      offset = skipWhitespace( offset, false );
      _expr = new ExpressionParser( _buffer, offset, _bufferEndOffset ).parse();
      _tokenEndOffset = _expr.getEndOffset();
    }
    else if( match( TokenType.Elif.getDirective(), offset ) )
    {
      offset += TokenType.Elif.getDirective().length();
      _tokenType = TokenType.Elif;

      offset = skipWhitespace( offset, false );
      _expr = new ExpressionParser( _buffer, offset, _bufferEndOffset ).parse();
      _tokenEndOffset = _expr.getEndOffset();
    }
    else if( match( TokenType.Else.getDirective(), offset ) )
    {
      offset += TokenType.Else.getDirective().length();
      _tokenType = TokenType.Else;
      _tokenEndOffset = offset;
    }
    else if( match( TokenType.Endif.getDirective(), offset ) )
    {
      offset += TokenType.Endif.getDirective().length();
      _tokenType = TokenType.Endif;
      _tokenEndOffset = offset;
    }
    else if( match( TokenType.Define.getDirective(), offset ) )
    {
      offset += TokenType.Define.getDirective().length();
      _tokenType = TokenType.Define;

      offset = skipWhitespace( offset, false );
      _expr = new ExpressionParser( _buffer, offset, _bufferEndOffset ).parse();
      _tokenEndOffset = _expr.getEndOffset();
    }
    else if( match( TokenType.Undef.getDirective(), offset ) )
    {
      offset += TokenType.Undef.getDirective().length();
      _tokenType = TokenType.Undef;

      offset = skipWhitespace( offset, false );
      _expr = new ExpressionParser( _buffer, offset, _bufferEndOffset ).parse();
      _tokenEndOffset = _expr.getEndOffset();
    }
    else if( match( TokenType.Error.getDirective(), offset ) )
    {
      offset += TokenType.Error.getDirective().length();
      _tokenType = TokenType.Error;

      offset = skipWhitespace( offset, false );
      _expr = new ExpressionParser( _buffer, offset, _bufferEndOffset ).parse();
      _tokenEndOffset = _expr.getEndOffset();
    }
    else if( match( TokenType.Warning.getDirective(), offset ) )
    {
      offset += TokenType.Warning.getDirective().length();
      _tokenType = TokenType.Warning;

      offset = skipWhitespace( offset, false );
      _expr = new ExpressionParser( _buffer, offset, _bufferEndOffset ).parse();
      _tokenEndOffset = _expr.getEndOffset();
    }
    else
    {
      return false;
    }
    return true;
  }

  private boolean match( String str, int offset )
  {
    if( _buffer.length() < offset + str.length() )
    {
      return false;
    }

    int i;
    for( i = 0; i < str.length(); i++ )
    {
      if( str.charAt( i ) != _buffer.charAt( offset + i ) )
      {
        return false;
      }
    }

    offset += i;
    return offset >= _bufferEndOffset ||
           !Character.isJavaIdentifierPart( _buffer.charAt( offset ) );
  }

  private char charAt( int i )
  {
    return _buffer.charAt( i );
  }

  private void makeSourceCodeToken( int offset )
  {
    _tokenType = TokenType.Source;
    _tokenEndOffset = skipSourceCode( offset );
  }

  private int skipSourceCode( int offset )
  {
    if( offset >= _bufferEndOffset )
    {
      return _bufferEndOffset;
    }

    int pos = offset;
    char c = _buffer.charAt( pos );

    while( !isCommentStart( pos ) &&
           c != '\n' &&
           c != '\r' &&
           c != '\f' &&
           c != '"' &&
           c != '\'' &&
           c != '#' )
    {
      if( ++pos == _bufferEndOffset )
      {
        return pos;
      }
      c = _buffer.charAt( pos );
    }

    return pos;
  }

  private boolean isCommentStart( int pos )
  {
    if( _buffer.charAt( pos ) == '/' )
    {
      if( pos + 1 < _bufferEndOffset )
      {
        char nextChar = charAt( pos + 1 );
        return nextChar == '/' || nextChar == '*';
      }
    }
    return false;
  }

  private int skipWhitespace( int offset, boolean multiLine )
  {
    if( offset >= _bufferEndOffset )
    {
      return _bufferEndOffset;
    }

    int pos = offset;
    char c = charAt( pos );

    while( (c == ' ' || c == '\t') || multiLine && (c == '\n' || c == '\r' || c == '\f') )
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

  private int getLineTerminator( int offset )
  {
    int pos = offset;

    while( pos < _bufferEndOffset )
    {
      char c = charAt( pos );
      if( c == '\r' || c == '\n' )
      {
        break;
      }
      pos++;
    }

    return pos;
  }

  private int getClosingComment( int offset )
  {
    int pos = offset;

    while( pos < _bufferEndOffset - 1 )
    {
      char c = charAt( pos );
      if( c == '*' && (charAt( pos + 1 )) == '/' )
      {
        break;
      }
      pos++;
    }

    return pos + 2;
  }

  private int getClosingQuote( int offset, char quoteChar )
  {
    if( offset >= _bufferEndOffset )
    {
      return _bufferEndOffset;
    }

    int pos = offset;
    char c = charAt( pos );

    while( true )
    {
      while( c != quoteChar && c != '\n' && c != '\r' && c != '\\' )
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
      else if( c == quoteChar )
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

  private int getTextBlockEnd( int offset )
  {
    int pos = offset;

    while( (pos = getClosingQuote( pos + 1, '"' )) < _bufferEndOffset )
    {
      if( pos + 1 < _bufferEndOffset && charAt( pos + 1 ) == '"' && charAt( pos ) == '"' )
      {
        pos += 2;
        break;
      }
    }

    return pos;
  }

  private void addError( @SuppressWarnings("SameParameterValue") String message, int pos )
  {
    if( JavacPlugin.instance() == null )
    {
      // IDE
      return;
    }

    IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ),
      new JCDiagnostic.SimpleDiagnosticPosition( pos ),
      "proc.messager", message );
  }
}
