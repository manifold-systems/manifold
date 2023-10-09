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

package manifold.sql.rt.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Use this to extract the list of commands from a SQL Script.
 */
public class SqlScriptParser
{
  private static final int EOF = -2;

  private final String _script;
  private final List<String> _commands;
  private final boolean _goAsSeparator;
  private int _pos;

  public static List<String> getCommands( String script )
  {
    return getCommands( script, false );
  }
  public static List<String> getCommands( String script, boolean goAsSeparator )
  {
    return new SqlScriptParser( script, goAsSeparator ).parse( script );
  }

  private SqlScriptParser( String script, boolean goAsSeparator )
  {
    _pos = -1;
    _script = script;
    _goAsSeparator = goAsSeparator;
    _commands = new ArrayList<>();
  }

  private boolean goAsSeparator()
  {
    return _goAsSeparator;
  }

  private List<String> parse( String script )
  {
    next();
    while( !isEof() )
    {
      parseCommand();
    }
    return _commands;
  }

  private void parseCommand()
  {
    int pos = _pos;
    int blockDepth = 0;
    int endCommandPos = -1;
    while( !isEof() )
    {
      //noinspection StatementWithEmptyBody
      while( eatComments() || eatWhitespace() );


      if( match( ';' ) )
      {
        if( blockDepth == 0 )
        {
          endCommandPos = _pos - 1;
          break;
        }
      }

      String word = matchWord();

      if( goAsSeparator() && word.equalsIgnoreCase( "GO" ) )
      {
        // sql server 'GO' is like a ';' separator
        if( blockDepth == 0 )
        {
          endCommandPos = _pos - 2;
          break;
        }
      }
      else if( is$$Quote( word ) )
      {
        // Postgres uses '$$' as a quote. Syntax is actually $[word]$ e.g., $myquote$ is considered a custom quote character.
        // Used mostly for function body quoting. Postgres...
        eatUntil( word );
      }
      else if( word.equalsIgnoreCase( "BEGIN" ) )
      {
        blockDepth++;
      }
      else if( word.equalsIgnoreCase( "CASE" ) )
      {
        blockDepth++;
      }
      else if( word.equalsIgnoreCase( "END" ) )
      {
        boolean isReallyEND = true;
        if( !isEof() && ch() == ' ' )
        {
          next();
          String endWhat = matchWord();
          if( endWhat.equalsIgnoreCase( "IF" ) || endWhat.equalsIgnoreCase( "LOOP" ) )
          {
            isReallyEND = false;
          }
        }

        if( isReallyEND )
        {
          blockDepth--;
          if( blockDepth < 0 )
          {
            throw new IllegalStateException( "Unbalanced BEGIN/CASE END (see [*])\n\n" +
              new StringBuilder( _script ).insert( _pos - 3, " [*]" ).toString() );
          }
        }
      }
      else if( word.equalsIgnoreCase( "SEPARATOR" ) )
      {
        // handle  SEPARATOR ';'  case
        while( eatComments() || eatWhitespace() );
        matchStringLiteral();
      }
      matchNonWord();
    }
    String command = _script.substring( pos, isEof() ? _script.length() : endCommandPos > 0 ? endCommandPos : _pos );
    command = command.trim();
    if( !command.isEmpty() )
    {
      _commands.add( command );
    }
  }

  private boolean is$$Quote( String word )
  {
    return word.length() > 1 && word.startsWith( "$" ) && word.endsWith( "$" );
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

  private boolean eatMultiLineComment()
  {
    return eatBetweenTokens( "/*", "*/" );
  }

  private boolean eatBetweenTokens( String open, String close )
  {
    boolean comment = false;
    while( !isEof() )
    {
      if( comment )
      {
        if( match( close ) )
        {
          return true;
        }
        next();
      }
      else if( match( open ) )
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

  private boolean eatUntil( String close )
  {
    boolean comment = false;
    while( !isEof() )
    {
      if( match( close ) )
      {
        return true;
      }
      next();
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
    if( _pos + 1 == _script.length() )
    {
      _pos = EOF;
      return;
    }

    _pos++;
  }

  private char ch()
  {
    return _script.charAt( _pos );
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

    if( _script.regionMatches( true, _pos, s, 0, s.length() ) )
    {
      if( !peek )
      {
        _pos += s.length();
        if( _pos == _script.length() )
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
    return matchWord( false );
  }
  private String matchWord( boolean peek )
  {
    if( isEof() )
    {
      return "";
    }

    int savePos = _pos;

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

    if( peek )
    {
      _pos = savePos;
    }
    return sb.toString();
  }

  private String matchStringLiteral()
  {
    if( isEof() )
    {
      return "";
    }

    if( match( '\'' ) )
    {
      StringBuilder sb = new StringBuilder();
      while( !match( '\'' ) )
      {
        sb.append( ch() );
        next();
      }
      return sb.toString();
    }
    return null;
  }

  private String matchNonWord()
  {
    if( isEof() )
    {
      return "";
    }

    while( eatWhitespace() || eatComments() );

    StringBuilder sb = new StringBuilder();
    while( !isEof() && ch() != ';' && !Character.isJavaIdentifierStart( ch() ) &&
      !match( "--", true ) && !match( "/*", true ) )
    {
      sb.append( ch() );
      next();
    }
    return sb.toString();
  }
}
