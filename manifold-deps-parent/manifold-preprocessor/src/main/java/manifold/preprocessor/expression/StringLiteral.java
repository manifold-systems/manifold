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

import manifold.preprocessor.definitions.Definitions;

public class StringLiteral extends TerminalExpression
{
  private final String _rawString;

  StringLiteral( String rawString, int start, int end )
  {
    super( start, end );
    if( rawString == null )
    {
      throw new IllegalArgumentException( "Null string not allowed" );
    }
    _rawString = rawString;
  }

  @Override
  public boolean evaluate( Definitions definitions )
  {
    return true;
  }

  @Override
  public String getValue( Definitions definitions )
  {
    String unquoted = _rawString.substring( 1, _rawString.length() > 1 && _rawString.endsWith( "\"" )
                                            ? _rawString.length() - 1
                                            : _rawString.length() );
    return unescape( unquoted );
  }

  public String toString()
  {
    return _rawString;
  }

  private String unescape( String rawString )
  {
    StringBuilder result = new StringBuilder();
    int length = rawString.length();
    boolean escaped = false;
    for( int idx = 0; idx < length; idx++ )
    {
      char ch = rawString.charAt( idx );
      if( !escaped )
      {
        if( ch == '\\' )
        {
          escaped = true;
        }
        else
        {
          result.append( ch );
        }
      }
      else
      {
        int octalEscapeMaxLength = 2;
        switch( ch )
        {
          case 'n':
            result.append( '\n' );
            break;

          case 'r':
            result.append( '\r' );
            break;

          case 'b':
            result.append( '\b' );
            break;

          case 't':
            result.append( '\t' );
            break;

          case 'f':
            result.append( '\f' );
            break;

          case '\'':
            result.append( '\'' );
            break;

          case '\"':
            result.append( '\"' );
            break;

          case '\\':
            result.append( '\\' );
            break;

          case 'u':
            if( idx + 4 < length )
            {
              try
              {
                int code = Integer.parseInt( rawString.substring( idx + 1, idx + 5 ), 16 );
                idx += 4;
                result.append( (char)code );
              }
              catch( NumberFormatException e )
              {
                result.append( "\\u" );
              }
            }
            else
            {
              result.append( "\\u" );
            }
            break;

          case '0':
          case '1':
          case '2':
          case '3':
            octalEscapeMaxLength = 3;
          case '4':
          case '5':
          case '6':
          case '7':
            int escapeEnd = idx + 1;
            while( escapeEnd < length && escapeEnd < idx + octalEscapeMaxLength && isOctalDigit( rawString.charAt( escapeEnd ) ) )
            {
              escapeEnd++;
            }
            try
            {
              result.append( (char)Integer.parseInt( rawString.substring( idx, escapeEnd ), 8 ) );
            }
            catch( NumberFormatException e )
            {
              throw new RuntimeException( "Couldn't parse " + rawString.substring( idx, escapeEnd ), e ); // shouldn't happen
            }
            //noinspection AssignmentToForLoopParameter
            idx = escapeEnd - 1;
            break;

          default:
            result.append( ch );
            break;
        }
        escaped = false;
      }
    }

    if( escaped )
    {
      result.append( '\\' );
    }

    return result.toString();
  }

  private boolean isOctalDigit( char c )
  {
    return '0' <= c && c <= '7';
  }
}
