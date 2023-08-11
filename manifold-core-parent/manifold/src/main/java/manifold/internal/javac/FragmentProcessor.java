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
import manifold.api.util.fingerprint.Fingerprint;

/**
 * Process embedded type fragments. Dynamically adds virtual resource files to the type system as they are encountered,
 * thus enabling native resource types to be embedded in Java source.
 */
public class FragmentProcessor
{
  public static final String FRAGMENT_START = "[";
  public static final String FRAGMENT_END = "/]";
  public static final char SCHEMA_NAME_SEPARATOR = ':';

  private final static FragmentProcessor INSTANCE = new FragmentProcessor();
  public static final String ANONYMOUS_FRAGMENT_PREFIX = "Fragment_";

  public static FragmentProcessor instance()
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

  /**
   * [>Foo.graphql<] query Persons { name address }
   */
  void processComment( JavaFileObject sourceFile, int pos, String comment, Tokens.Comment.CommentStyle style )
  {
    Fragment f = parseFragment( pos, comment, HostKind.from( style ) );
    if( f != null )
    {
      JavacPlugin.instance().registerType( sourceFile, f.getScope(), f.getOffset(), f.getName(), f.getExt(), f.getHostKind(), f._content );
    }
  }

  void processString( JavaFileObject sourceFile, int pos, String chars )
  {
    Fragment f = parseFragment( pos, chars,
      chars.length() > 3 && chars.charAt( 1 ) == '"'
      ? HostKind.TEXT_BLOCK_LITERAL
      : HostKind.DOUBLE_QUOTE_LITERAL );
    if( f != null )
    {
      JavacPlugin.instance().registerType( sourceFile, f.getScope(), f.getOffset(), f.getName(), f.getExt(), f.getHostKind(), f._content );
    }
  }

  public Fragment parseFragment( int pos, String chars, HostKind hostKind )
  {
    int index = 0;
    int end = chars.length();
    boolean isString = false;
    switch( hostKind )
    {
      case LINE_COMMENT:
        index += 2; // skip '//'
        break;
      case BLOCK_COMMENT:
        index += 2; // skip '/*'
        end -= 2;   // end before '*/'
        break;
      case JAVADOC_COMMENT:
        index += 3; // skip '/**'
        end -= 2;   // end before '*/'
        break;
      case DOUBLE_QUOTE_LITERAL:
        isString = true;
        index += 1; // skip "
        end -= 1;   // end before terminating "
        break;
      case TEXT_BLOCK_LITERAL:
        isString = true;
        index += 3; // skip """
        end -= 3;   // end before terminating """
        break;
    }
    index = skipSpaces( chars, index, end );
    if( index + 1 >= end )
    {
      return null;
    }

    if( chars.startsWith( FRAGMENT_START, index ) )
    {
      index += FRAGMENT_START.length();
      index = skipSpaces( chars, index, end );
      String name;
      if( isString && index < end && chars.charAt( index ) == '.' )
      {
        // Name is optional if fragment is in a String literal e.g., "[>.sql<] blah blah" // just the dot is ok
        //(note the reason why the dot is needed for anonymity is so multi-extension names can be distinguished
        //esp. for template languages e.g., .html.mtl)
        name = "";
      }
      else
      {
        name = parseName( chars, index, end );
      }

      if( name != null )
      {
        index += name.length();
        List<String> exts = new ArrayList<>();
        index = parseExtensions( chars, index, end, exts );
        if( index < end && !exts.isEmpty() )
        {
          index = skipSpaces( chars, index, end );
          if( index < end )
          {
            String schema = null;
            if( SCHEMA_NAME_SEPARATOR == chars.charAt( index ) )
            {
              index++;
              index = skipSpaces( chars, index, end );
              schema = parseName( chars, index, end );
              index += (schema == null ? 0 : schema.length());
              index = skipSpaces( chars, index, end );
            }
            if( index + 1 < end && chars.startsWith( FRAGMENT_END, index ) )
            {
              index += FRAGMENT_END.length();
              String content = chars.substring( index, end );
              name = isString ? handleAnonymousName( name, content ) : name;
              return new Fragment( pos + index, schema, makeBaseName( name, exts ), exts.get( exts.size() - 1 ), hostKind, content );
            }
          }
        }
      }
    }
    return null;
  }

  private String handleAnonymousName( String name, String content )
  {
    if( name.isEmpty() )
    {
      // name must be uniquely deterministic wrt content

      Fingerprint fingerprint = new Fingerprint( content );
      String suffix = fingerprint.toString();
      suffix = suffix.replace( '-', '_');
      name = ANONYMOUS_FRAGMENT_PREFIX + suffix;
    }
    return name;
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
    private final String _scope;
    private final String _name;
    private final String _ext;
    private final String _content;
    private final HostKind _hostKind;

    Fragment( int offset, String scope, String name, String ext, HostKind hostKind, String content )
    {
      _offset = offset;
      _scope = scope;
      _name = name;
      _ext = ext;
      _hostKind = hostKind;
      _content = content;
    }

    public int getOffset()
    {
      return _offset;
    }

    public String getScope()
    {
      return _scope;
    }

    public String getName()
    {
      return _name;
    }

    public String getExt()
    {
      return _ext;
    }

    @SuppressWarnings("WeakerAccess")
    public HostKind getHostKind()
    {
      return _hostKind;
    }

    public String getContent()
    {
      return _content;
    }
  }
}
