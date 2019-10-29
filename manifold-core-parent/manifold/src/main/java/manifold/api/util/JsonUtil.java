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

package manifold.api.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.Map;

/**
 */
public class JsonUtil
{
  private static final String XML_ELEM_CONTENT = "textContent";

  public static String makeIdentifier( String name )
  {
    String identifier = ReservedWordMapping.getIdentifierForName( name );
    if( !identifier.equals( name ) )
    {
      return identifier;
    }

    StringBuilder sb = new StringBuilder();
    for( int i = 0; i < name.length(); i++ )
    {
      char c = name.charAt( i );
      if( i == 0 && Character.isDigit( c ) )
      {
        sb.append( '_' ).append( c );
      }
      else if( c == '_' || c == '$' || Character.isLetterOrDigit( c ) )
      {
        sb.append( c );
      }
      else
      {
        sb.append( '_' );
      }
    }
    identifier = makeCorrections( sb );
    return identifier;
  }

  private static String makeCorrections( StringBuilder sb )
  {
    String identifier = sb.toString();
    if( isAllUnderscores( identifier ) )
    {
      identifier = "_" + identifier.length();
    }
    return identifier;
  }

  private static boolean isAllUnderscores( String result )
  {
    for( int i = 0; i < result.length(); i++ )
    {
      if( result.charAt( i ) != '_' )
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Serializes this Map instance to a JSON formatted String
   */
  public static String toJson( Map thisMap )
  {
    StringBuilder sb = new StringBuilder();
    toJson( thisMap, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this Map instance into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static void toJson( Map thisMap, StringBuilder sb, int indent )
  {
    int iKey = 0;
    if( isNewLine( sb ) )
    {
      indent( sb, indent );
    }
    sb.append( "{\n" );
    if( thisMap.size() > 0 )
    {
      for( Object key : thisMap.keySet() )
      {
        indent( sb, indent + 2 );
        sb.append( '\"' ).append( key ).append( '\"' ).append( ": " );
        Object value = thisMap.get( key );
        if( value instanceof Map )
        {
          toJson( (Map)value, sb, indent + 2 );
        }
        else if( value instanceof Iterable )
        {
          listToJson( sb, indent + 2, (Iterable)value );
        }
        else
        {
          appendValue( sb, value );
        }
        appendCommaNewLine( sb, iKey < thisMap.size() - 1 );
        iKey++;
      }
    }
    indent( sb, indent );
    sb.append( "}" );
  }

  /**
   * Build a JSON string from the specified {@code value}. The {@code value} must be a valid JSON value:
   * <lu>
   *   <li>primitive, boxed primitive, or {@code String}</li>
   *   <li>{@code Iterable} of JSON values</li>
   *   <li>{@code Map} of JSON values</li>
   * </lu>
   * @return A JSON String reflecting the specified {@code value}
   */
  public static String toJson( Object value )
  {
    StringBuilder target = new StringBuilder();
    toJson( target, 0, value );
    return target.toString();
  }
  /**
   * Build a JSON string in the specified {@code target} from the specified {@code value} with the provided left
   * {@code margin}. The {@code value} must be a valid JSON value:
   * <lu>
   *   <li>primitive, boxed primitive, or {@code String}</li>
   *   <li>{@code Iterable} of JSON values</li>
   *   <li>{@code Map} of JSON values</li>
   * </lu>
   */
  public static void toJson( StringBuilder target, int margin, Object value )
  {
    if( value instanceof Pair )
    {
      value = ((Pair)value).getSecond();
    }
    if( value instanceof Map )
    {
      toJson( ((Map)value), target, margin );
    }
    else if( value instanceof Iterable )
    {
      listToJson( target, margin, (Iterable)value );
    }
    else
    {
      appendValue( target, value );
    }
  }

  private static boolean isNewLine( StringBuilder sb )
  {
    return sb.length() > 0 && sb.charAt( sb.length() - 1 ) == '\n';
  }

  public static void listToJson( StringBuilder sb, int indent, Iterable value )
  {
    sb.append( '[' );
    int i = 0;
    for( Iterator iter = value.iterator(); iter.hasNext(); )
    {
      Object comp = iter.next();
      if( i == 0 )
      {
        sb.append( "\n" );
      }
      if( comp instanceof Map )
      {
        toJson( (Map)comp, sb, indent + 2 );
      }
      else if( comp instanceof Iterable )
      {
        listToJson( sb, indent + 2, (Iterable)comp );
      }
      else
      {
        indent( sb, indent + 2 );
        appendValue( sb, comp );
      }
      appendCommaNewLine( sb, iter.hasNext() );
      i++;
    }
    indent( sb, indent );
    sb.append( "]" );
  }

  /**
   * Serializes a JSON-compatible List into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static String listToJson( Iterable list )
  {
    StringBuilder sb = new StringBuilder();
    listToJson( sb, 0, list );
    return sb.toString();
  }


  private static void appendCommaNewLine( StringBuilder sb, boolean bComma )
  {
    if( bComma )
    {
      sb.append( ',' );
    }
    sb.append( "\n" );
  }

  private static void indent( StringBuilder sb, int indent )
  {
    int i = 0;
    while( i < indent )
    {
      sb.append( ' ' );
      i++;
    }
  }

  public static StringBuilder appendValue( StringBuilder sb, Object comp )
  {
    if( comp instanceof String )
    {
      sb.append( '\"' );
      sb.append( ManEscapeUtil.escapeForJavaStringLiteral( (String)comp ) );
      sb.append( '\"' );
    }
    else if( comp instanceof Integer ||
             comp instanceof Long ||
             comp instanceof Double ||
             comp instanceof Float ||
             comp instanceof Short ||
             comp instanceof Character ||
             comp instanceof Byte ||
             comp instanceof Boolean )
    {
      sb.append( comp );
    }
    else if( comp == null )
    {
      sb.append( "null" );
    }
    else
    {
      throw new IllegalStateException( "Unsupported expando type: " + comp.getClass() );
    }
    return sb;
  }

  public static String toXml( Object jsonValue )
  {
    StringBuilder sb = new StringBuilder();
    if( jsonValue instanceof Map )
    {
      toXml( jsonValue, null, sb, 0 );
    }
    else if( jsonValue instanceof Iterable )
    {
      toXml( jsonValue, "list", sb, 0 );
    }
    else
    {
      toXml( jsonValue, "item", sb, 0 );
    }
    return sb.toString();
  }

  public static void toXml( Object jsonValue, String name, StringBuilder target, int indent )
  {
    if( jsonValue instanceof Map )
    {
      if( name == null )
      {
        Map map = (Map)jsonValue;
        if( map.size() == 1 )
        {
          // single entry with no name implies root, defer to the root
          Object rootKey = map.keySet().iterator().next();
          Object rootValue = map.get( rootKey );
          if( rootValue instanceof Pair )
          {
            rootValue = ((Pair)rootValue).getSecond();
          }
          toXml( rootValue, rootKey.toString(), target, indent );
          return;
        }
        else
        {
          name = "object";
        }
      }
      toXml( (Map)jsonValue, name, target, indent );
    }
    else if( jsonValue instanceof Iterable )
    {
      toXml( (Iterable)jsonValue, name, target, indent );
    }
    else
    {
      toXml( String.valueOf( jsonValue ), name, target, indent );
    }
  }

  /**
   * Serializes this {@link Map} instance into an XML formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param name   The name of the root element to nest the Map XML
   * @param target A {@link StringBuilder} to write the XML in
   * @param indent The margin of spaces to indent the XML
   */
  private static void toXml( Map bindings, String name, StringBuilder target, int indent )
  {
    indent( target, indent );
    target.append( '<' ).append( name );
    if( bindings.size() > 0 )
    {
      for( Object key: bindings.keySet() )
      {
        Object value = bindings.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }

        if( !(value instanceof Map) && !(value instanceof Iterable) && !key.equals( XML_ELEM_CONTENT ) )
        {
          target.append( " " ).append( key ).append( "=\"" ).append( value ).append( '"' );
        }
      }
      int count = 0;
      for( Object key: bindings.keySet() )
      {
        Object value = bindings.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }

        if( value instanceof Map )
        {
          if( count == 0 )
          {
            target.append( ">\n" );
          }

          toXml( (Map)value, key.toString(), target, indent + 2 );

          count++;
        }
        else if( value instanceof Iterable )
        {
          if( count == 0 )
          {
            target.append( ">\n" );
          }

          toXml( (Iterable)value, key.toString(), target, indent + 2 );

          count++;
        }
        else if( value instanceof String && key.equals( XML_ELEM_CONTENT ) )
        {
          if( count == 0 )
          {
            target.append( ">\n" );
          }

          indent( target, indent + 2 );
          target.append( value ).append( "\n" );

          count++;
        }
      }
      
      if( count == 0 )
      {
        target.append( "/>\n" );
      }
      else
      {
        indent( target, indent );
        target.append( "</" ).append( name ).append( ">\n" );
      }
    }
    else
    {
      target.append( "/>\n" );
    }
  }

  private static void toXml( Iterable value, String name, StringBuilder target, int indent )
  {
    for( Object comp: value )
    {
      if( comp instanceof Pair )
      {
        comp = ((Pair)comp).getSecond();
      }

      if( comp instanceof Map )
      {
        toXml( (Map)comp, name, target, indent );
      }
      else if( comp instanceof Iterable )
      {
        toXml( (Iterable)comp, name, target, indent );
      }
      else
      {
        toXml( String.valueOf( comp ), name, target, indent );
      }
    }
  }

  private static void toXml( String value, String name, StringBuilder target, int indent )
  {
    indent( target, indent );
    target.append( '<' ).append( name ).append( ">" );
    target.append( value );
    target.append( "</" ).append( name ).append( ">\n" );
  }

  public static <E extends Map<String, Object>> Object deepCopyValue( Object value, Function<Integer, E> bindingsSupplier )
  {
    if( value instanceof Map )
    {
      Map<String, Object> dataMap = (Map)value;
      Map<String, Object> copy = bindingsSupplier.apply( dataMap.size() );
      dataMap.forEach( ( k, v ) -> copy.put( k, deepCopyValue( v, bindingsSupplier ) ) );
      return copy;
    }

    if( value instanceof Iterable )
    {
      //noinspection unchecked
      Iterable<Object> list = (Iterable<Object>)value;
      List<Object> copy = new ArrayList<>();
      list.forEach( e -> copy.add( deepCopyValue( e, bindingsSupplier ) ) );
      return copy;
    }

    return value;
  }
}
