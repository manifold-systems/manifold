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

package manifold.internal.javac;

import com.sun.tools.javac.parser.Tokens;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;

/**
 * Process embedded type fragments. Dynamically adds virtual resource files to the type system as they are encountered,
 * thus enabling native resource types to be embedded in Java source.
 */
public class CommentProcessor
{
  private final static CommentProcessor INSTANCE = new CommentProcessor();

  public static CommentProcessor instance()
  {
    return INSTANCE;
  }

  /*[>Foo.graphql<]
      query Persons {
        name
        address
      }
  */
  /*[>Foo.graphql<] query Persons { name address }*/
  //[>Foo.graphql<] query Persons { name address }
  /**[>Foo.graphql<] query Persons { name address }*/
  void processComment( JavaFileObject sourceFile, int pos, String comment, Tokens.Comment.CommentStyle style )
  {
    Fragment f = parseFragment( pos, comment, Style.from( style ) );
    if( f != null )
    {
      JavacPlugin.instance().registerType( sourceFile, f.getOffset(), f.getName(), f.getExt(), f._content );
    }
  }

  public enum Style
  {
    LINE, BLOCK, JAVADOC;

    static Style from( Tokens.Comment.CommentStyle s )
    {
      switch( s )
      {
        case LINE:
          return LINE;
        case BLOCK:
          return BLOCK;
        case JAVADOC:
          return JAVADOC;
      }
      throw new IllegalStateException();
    }
  }

  @SuppressWarnings("WeakerAccess")
  public Fragment parseFragment( int pos, String comment, Style style )
  {
    int index = 0;
    int end = comment.length();
    switch( style )
    {
      case LINE:
        index += 2; // skip '//'
        break;
      case BLOCK:
        index += 2; // skip '/*'
        end -= 2;   // end before '*/'
        break;
      case JAVADOC:
        index += 3; // skip '/**'
        end -= 2;   // end before '*/'
        break;
    }
    index = skipSpaces( comment, index, end );
    if( index+1 >= end )
    {
      return null;
    }

    if( '[' == comment.charAt( index++ ) &&
        '>' == comment.charAt( index++ ) )
    {
      index = skipSpaces( comment, index, end );
      String name = parseName( comment, index, end );
      if( name != null )
      {
        index += name.length();
        List<String> exts = new ArrayList<>();
        index = parseExtensions( comment, index, end, exts );
        if( index < end && !exts.isEmpty() )
        {
          index = skipSpaces( comment, index, end );
          if( index+1 < end &&
              '<' == comment.charAt( index++ ) &&
              ']' == comment.charAt( index++ ) )
          {
            String content = comment.substring( index, end );
            return new Fragment( pos + index, makeBaseName( name, exts ), exts.get( exts.size()-1 ), content );
          }
        }
      }
    }
    return null;
  }

  private int parseExtensions( String comment, int index, int end, List<String> exts )
  {
    while( index < end &&
           '.' == comment.charAt( index ) )
    {
      index++;
      String ext = parseName( comment, index, end );
      if( ext != null )
      {
        exts.add( ext );
        index += ext.length();
      }
      else
      {
        break;
      }
    }
    return index;
  }

  private String makeBaseName( String name, List<String> exts )
  {
    StringBuilder sb = new StringBuilder( name );
    for( int i = 0; i < exts.size() - 1; i++ )
    {
      sb.append( '.' ).append( exts.get( i ) );
    }
    return sb.toString();
  }

  private String parseName( String comment, int index, int end )
  {
    StringBuilder sb = new StringBuilder();
    int start = index;
    char c;
    while( index < end &&
           (index == start
            ? Character.isJavaIdentifierStart( c = comment.charAt( index ) )
            : Character.isJavaIdentifierPart( c = comment.charAt( index ) )) )
    {
      index++;
      sb.append( c );
    }
    return index == start || index == end
           ? null
           : sb.toString();
  }

  private int skipSpaces( String comment, int index, int end )
  {
    while( index < end &&
           Character.isWhitespace( comment.charAt( index ) ) )
    {
      index++;
    }
    return index;
  }

  public static class Fragment
  {
    private final int _offset;
    private final String _name;
    private final String _ext;
    private final String _content;

    Fragment( int offset, String name, String ext, String content )
    {
      _offset = offset;
      _name = name;
      _ext = ext;
      _content = content;
    }

    public int getOffset()
    {
      return _offset;
    }

    public String getName()
    {
      return _name;
    }

    public String getExt()
    {
      return _ext;
    }

    public String getContent()
    {
      return _content;
    }
  }
}
