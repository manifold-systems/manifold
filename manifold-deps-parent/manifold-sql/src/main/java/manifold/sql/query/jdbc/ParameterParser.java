/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.query.jdbc;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses :xxx parameters
 */
public class ParameterParser
{
  private static final int EOF = -2;

  private final String _query;
  private int _pos;

  public static List<ParamInfo> getParameters( String query )
  {
    return new ParameterParser( query ).parse();
  }

  private ParameterParser( String query )
  {
    _pos = -1;
    _query = query;
  }

  private List<ParamInfo> parse()
  {
    next();
    return parseParameters();
  }

  private List<ParamInfo> parseParameters()
  {
    List<ParamInfo> parameters = new ArrayList<>();
    while( !isEof() )
    {
      //noinspection StatementWithEmptyBody
      while( eatComments() || eatStrings() || eatEverythingButParamsStringsAndComments() );

      if( match( ':' ) && !match( ':' ) ) // avoid :: operator (postgres)
      {
        int pos = _pos - 1;
        String word = matchWord();
        if( !word.isEmpty() )
        {
          parameters.add( new ParamInfo( pos, ':' + word ) );
        }
      }
    }
    return parameters;
  }

  private boolean eatWhitespace()
  {
    boolean found = false;
    while( !isEof() && Character.isWhitespace( ch() ) )
    {
      found = true;
      next();
    }
    return found;
  }

  private boolean eatComments()
  {
    return eatLineComment() || eatMultiLineComment();
  }

  private boolean eatStrings()
  {
    return eatQStrings() || eatStrings( '\'' ) || eatStrings( '"' );
  }

  // Include Oracle "Alternative Quoting Mechanism" for string literal matching.
  // Basically anything between the delimiters taken as-is, no escaping required:
  //  q'[My quote's 'quote']'
  // Where [ and ] can be any character.
  private boolean eatQStrings()
  {
    Character startLiteral = null;
    char endLiteral = 0;
    while( !isEof() )
    {
      if( startLiteral != null )
      {
        if( match( endLiteral + "'" ) )
        {
          return true;
        }
        next();
      }
      else if( match( "q'" ) || match( "Q'" ) )
      {
        startLiteral = ch();
        endLiteral = endLiteralFor( ch() );
      }
      else
      {
        break;
      }
    }
    if( startLiteral != null )
    {
      throw new RuntimeException( "String literal missing end quote" );
    }
    return false;
  }

  private char endLiteralFor( char ch )
  {
    switch( ch )
    {
      case '[':
        return ']';
      case '{':
        return '}';
      case '(':
        return ')';
      case '<':
        return '>';
      default:
        return ch;
    }
  }

  private boolean eatStrings( char quote )
  {
    boolean isString = false;
    while( !isEof() )
    {
      if( isString )
      {
        // support \x escaping
        if( !match( '\\') && match( quote ) )
        {
          // support double quote escaping
          if( !match( quote ) )
          {
            return true;
          }
        }
        next();
      }
      else if( match( quote ) )
      {
        isString = true;
      }
      else
      {
        break;
      }
    }
    if( isString )
    {
      throw new RuntimeException( "String literal missing end quote" );
    }
    return false;
  }

  private boolean eatMultiLineComment()
  {
    boolean comment = false;
    while( !isEof() )
    {
      if( comment )
      {
        if( match( "*/" ) )
        {
          return true;
        }
        next();
      }
      else if( match( "/*" ) )
      {
        comment = true;
      }
      else
      {
        break;
      }
    }
    if( comment )
    {
      throw new RuntimeException( "Comment missing `*/`" );
    }
    return false;
  }

  private boolean eatLineComment()
  {
    boolean comment = false;
    while( !isEof() )
    {
      if( comment )
      {
        if( match( '\n' ) )
        {
          return true;
        }
        next();
      }
      else if( match( "--" ) )
      {
        comment = true;
      }
      else
      {
        break;
      }
    }
    return false;
  }

  private boolean isEof()
  {
    return _pos == EOF;
  }

  private void next()
  {
    if( isEof() )
    {
      return;
    }
    if( ++_pos == _query.length() )
    {
      _pos = EOF;
    }
  }

  private char ch()
  {
    return ch( 0 );
  }
  private char ch( int lookahead )
  {
    return _query.charAt( _pos + lookahead );
  }

  private boolean match( char c )
  {
    if( isEof() )
    {
      return false;
    }

    if( ch() == c )
    {
      next();
      return true;
    }
    return false;
  }

  private boolean match( String s )
  {
    return match( s, false );
  }
  private boolean match( String s, boolean peek )
  {
    if( isEof() )
    {
      return false;
    }

    if( _query.startsWith( s, _pos ) )
    {
      if( !peek )
      {
        _pos += s.length();
        if( _pos == _query.length() )
        {
          _pos = EOF;
        }
      }
      return true;
    }
    return false;
  }

  private String matchWord()
  {
    if( isEof() )
    {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    if( Character.isJavaIdentifierStart( ch() ) )
    {
      sb.append( ch() );
      next();
      while( !isEof() && Character.isJavaIdentifierPart( ch() ) )
      {
        sb.append( ch() );
        next();
      }
    }
    return sb.toString();
  }

  private boolean eatEverythingButParamsStringsAndComments()
  {
    int pos = _pos;
    while( !isEof() &&
      ch() != ':' &&
      !match( "q'", true ) && !match( "Q'", true ) && ch() != '\'' && ch() != '"' &&
      !match( "--", true ) && !match( "/*", true ) )
    {
      next();
    }
    return pos != _pos;
  }
}
