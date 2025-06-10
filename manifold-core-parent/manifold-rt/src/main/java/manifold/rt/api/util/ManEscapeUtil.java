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

package manifold.rt.api.util;

public class ManEscapeUtil
{
  /**
   * Escape any special characters in the string, using the Java escape syntax.
   * For example any tabs become \t, newlines become \n etc.
   *
   * @return the escaped string. Returns the original string unchanged if it
   * contains no special characters.
   */
  public static String escapeForJava( String string )
  {
    String result;
    StringBuffer resultBuffer = null;
    for( int i = 0, length = string.length(); i < length; i++ )
    {
      char ch = string.charAt( i );
      String escape = escapeForJava( ch );
      if( escape != null )
      {
        if( resultBuffer == null )
        {
          resultBuffer = new StringBuffer( string );
          resultBuffer.setLength( i );
        }
        resultBuffer.append( escape );
      }
      else if( resultBuffer != null )
      {
        resultBuffer.append( ch );
      }
    }
    result = (resultBuffer != null) ? resultBuffer.toString() : string;
    return result;
  }

  /**
   * Converts an escaped character code into a string literal expressing it, e.g. '\n' becomes "\\n".
   *
   * @param ch Escaped character code.
   *
   * @return The string expression of the character code, null if <code>ch</code> is not an escaped character.
   * Supports Unicode.
   */
  public static String escapeForJava( char ch )
  {
    String escape = escapeForJavaStringLiteral( ch );
    if( escape == null )
    {
      if( ch <= 31 || ch >= 127 )
      {
        escape = getUnicodeEscape( ch );
      }
    }
    return escape;
  }

  public static String escapeForJavaCharLiteral( char c )
  {
    switch( c )
    {
      case '\b':
        return "\\b";
      case '\t':
        return "\\t";
      case '\n':
        return "\\n";
      case '\f':
        return "\\f";
      case '\r':
        return "\\r";
      case '\"':
        return "\"";
      case '\'':
        return "\\'";
      case '\\':
        return "\\\\";
      default:
        if( c >= 32 && c <= 126 )
        {
          // Printable ASCII character
          return String.valueOf( c );
        }
        else
        {
          // Unicode escape for non-printable or non-ASCII character
          return String.format( "\\u%04x", (int)c );
        }
    }
  }

  public static String escapeForJavaStringLiteral( String strText )
  {
    return escapeForJavaStringLiteral( strText, 0, strText.length() );
  }

  public static String escapeForJavaStringLiteral( String strText, int iStart, int iEnd )
  {
    StringBuilder sb = new StringBuilder( strText.length() );
    for( int i = iStart; i < iEnd; i++ )
    {
      sb.append( escapeForJavaStringLiteral( strText.charAt( i ) ) );
    }
    return sb.toString();
  }

  public static String escapeForJavaStringLiteral( char ch )
  {
    switch( ch )
    {
      case '\b':
        return "\\b";
      case '\f':
        return "\\f";
      case '\n':
        return "\\n";
      case '\r':
        return "\\r";
      case '\t':
        return "\\t";
      case '\"':
        return "\\\"";
      case '\\':
        return "\\\\";
      default:
        return isPrintableAscii( ch )
               ? String.valueOf( ch )
               : String.format( "\\u%04x", (int)ch );
    }
  }

  private static boolean isPrintableAscii( char ch )
  {
    return ch >= ' ' && ch <= '~';
  }

  public static String getUnicodeEscape( char ch )
  {
    String strPrefix = "\\u";
    int iLen = strPrefix.length() + 4;
    String strHexValue = Integer.toHexString( ch );
    StringBuilder sb = new StringBuilder( iLen );
    sb.append( strPrefix );
    for( int i = 0, n = iLen - (strPrefix.length() + strHexValue.length()); i < n; i++ )
    {
      sb.append( '0' );
    }
    sb.append( strHexValue );
    return sb.toString();
  }

  public static String stripNewLinesAndExtraneousWhiteSpace( String s )
  {
    if( s == null )
    {
      return null;
    }

    StringBuilder result = new StringBuilder();
    boolean hitNewLine = false;
    boolean addedSpace = false;
    for( int i = 0; i < s.length(); i++ )
    {
      char c = s.charAt( i );
      if( c == '\n' )
      {
        hitNewLine = true;
      }
      else if( c == ' ' )
      {
        if( hitNewLine )
        {
          if( !addedSpace )
          {
            result.append( c );
            addedSpace = true;
          }
        }
        else
        {
          result.append( c );
        }
      }
      else
      {
        hitNewLine = false;
        addedSpace = false;
        result.append( c );
      }
    }
    return result.toString().trim();
  }

  public static String escapeForHTML( String string )
  {
    return escapeForHTML( string, true );
  }

  public static String escapeForHTML( String string, boolean escapeWhitespace )
  {
    if( string == null || string.length() == 0 )
    {
      return string;
    }
    StringBuilder resultBuffer = null;
    char last = 0;
    for( int i = 0, length = string.length(); i < length; i++ )
    {
      String entity = null;
      char ch = string.charAt( i );
      switch( ch )
      {
        case '<':
        {
          entity = "&lt;";
          break;
        }
        case ' ':
          if( last == ' ' && escapeWhitespace )
          {
            entity = "&nbsp;";
          }
          break;
        case '>':
          entity = "&gt;";
          break;
        case '&':
          entity = "&amp;";
          break;
        case '"':
          entity = "&quot;";
          break;
        case '\'':
          entity = "&#39";
          break;
        case '\n':
          if( escapeWhitespace )
          {
            entity = "<br>";
          }
          break;
        default:
          break;
      }
      if( entity != null )
      {
        if( resultBuffer == null )
        {
          resultBuffer = new StringBuilder( string );
          resultBuffer.setLength( i );
        }
        resultBuffer.append( entity );
      }
      else if( resultBuffer != null )
      {
        resultBuffer.append( ch );
      }
      last = ch;
    }
    return (resultBuffer != null) ? resultBuffer.toString() : string;
  }
}
