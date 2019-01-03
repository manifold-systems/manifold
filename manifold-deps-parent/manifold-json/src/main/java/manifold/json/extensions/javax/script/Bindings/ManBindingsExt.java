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

package manifold.json.extensions.javax.script.Bindings;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.Json;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.json.extensions.java.net.URL.ManUrlExt;
import manifold.util.JsonUtil;
import manifold.util.Pair;

/**
 * Extends {@link Bindings} with methods to transform the Bindings contents to JSON and XML and to conveniently use the
 * Bindings for JSON Web requests.
 */
@Extension
public class ManBindingsExt
{
  /**
   * Serializes this {@link Bindings} instance to a JSON formatted String
   *
   * @return This {@link Bindings} instance serialized to a JSON formatted String
   */
  public static String toJson( @This Bindings thiz )
  {
    StringBuilder sb = new StringBuilder();
    toJson( thiz, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this {@link Bindings} instance into a JSON formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param target A {@link StringBuilder} to write the JSON in
   * @param indent The margin of spaces to indent the JSON
   */
  public static void toJson( @This Bindings thiz, StringBuilder target, int indent )
  {
    int iKey = 0;
    if( isNewLine( target ) )
    {
      indent( target, indent );
    }
    if( thiz.size() > 0 )
    {
      target.append( "{\n" );
      for( String key: thiz.keySet() )
      {
        indent( target, indent + 2 );
        target.append( '\"' ).append( key ).append( '\"' ).append( ": " );
        Object value = thiz.get( key );
        JsonUtil.toJson( target, indent, value );
        appendCommaNewLine( target, iKey < thiz.size() - 1 );
        iKey++;
      }
    }
    indent( target, indent );
    target.append( "}" );
  }

  private static boolean isNewLine( StringBuilder sb )
  {
    return sb.length() > 0 && sb.charAt( sb.length() - 1 ) == '\n';
  }

  /**
   * Utility to serialize a {@link List} composed of JSON values, where a JSON value is one of:
   * <ul>
   * <li> a simple type such as a String, number, or boolean </li>
   * <li> a {@link Bindings} of property names to JSON values </li>
   * <li> a {@link List} composed of JSON values </li>
   * </ul>
   *
   * @param target A {@link StringBuilder} to write the JSON in
   * @param indent The margin of spaces to indent the JSON
   * @param value  A {@link List} composed of JSON values to serialize
   */
  public static void listToJson( StringBuilder target, int indent, List value )
  {
    target.append( '[' );
    if( value.size() > 0 )
    {
      target.append( "\n" );
      int iSize = value.size();
      int i = 0;
      while( i < iSize )
      {
        Object comp = value.get( i );
        if( comp instanceof Pair )
        {
          comp = ((Pair)comp).getSecond();
        }
        if( comp instanceof Bindings )
        {
          toJson( ((Bindings)comp), target, indent + 4 );
        }
        else if( comp instanceof List )
        {
          listToJson( target, indent + 4, (List)comp );
        }
        else
        {
          indent( target, indent + 4 );
          JsonUtil.appendValue( target, comp );
        }
        appendCommaNewLine( target, i < iSize - 1 );
        i++;
      }
    }
    indent( target, indent + 2 );
    target.append( "]" );
  }

  /**
   * Serializes a JSON-compatible List into a JSON formatted StringBuilder with the specified indent of spaces.
   * Same as calling {@link #listToJson(StringBuilder, int, List)} with no indentation and returns a String.
   */
  public static String listToJson( List list )
  {
    StringBuilder sb = new StringBuilder();
    listToJson( sb, 0, list );
    return sb.toString();
  }

  /**
   * Serializes this {@link Bindings} instance to XML nested in a root element named {@code "object"}
   *
   * @see #toXml(Bindings, String)
   * @see #toXml(Bindings, String, StringBuilder, int)
   */
  public static String toXml( @This Bindings thiz )
  {
    return toXml( thiz, "object" );
  }

  /**
   * Serializes this {@link Bindings} instance to XML with in a root element with the specified {@code name}
   *
   * @param name The name of the root element to nest the Bindings XML
   *
   * @see #toXml(Bindings, String, StringBuilder, int)
   */
  public static String toXml( @This Bindings thiz, String name )
  {
    StringBuilder sb = new StringBuilder();
    toXml( thiz, name, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this {@link Bindings} instance into an XML formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param name   The name of the root element to nest the Bindings XML
   * @param target A {@link StringBuilder} to write the XML in
   * @param indent The margin of spaces to indent the XML
   */
  public static void toXml( @This Bindings thiz, String name, StringBuilder target, int indent )
  {
    indent( target, indent );
    target.append( '<' ).append( name );
    if( thiz.size() > 0 )
    {
      target.append( ">\n" );
      for( String key: thiz.keySet() )
      {
        Object value = thiz.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }
        if( value instanceof Bindings )
        {
          toXml( ((Bindings)value), key, target, indent + 2 );
        }
        else if( value instanceof List )
        {
          int len = ((List)value).size();
          indent( target, indent + 2 );
          target.append( "<" ).append( key );
          if( len > 0 )
          {
            target.append( ">\n" );
            for( Object comp: (List)value )
            {
              if( comp instanceof Pair )
              {
                comp = ((Pair)comp).getSecond();
              }

              if( comp instanceof Bindings )
              {
                toXml( ((Bindings)comp), "li", target, indent + 4 );
              }
              else
              {
                indent( target, indent + 4 );
                target.append( "<li>" ).append( comp ).append( "</li>\n" );
              }
            }
            indent( target, indent + 2 );
            target.append( "</" ).append( key ).append( ">\n" );
          }
          else
          {
            target.append( "/>\n" );
          }
        }
        else
        {
          indent( target, indent + 2 );
          target.append( '<' ).append( key ).append( ">" );
          target.append( value );
          target.append( "</" ).append( key ).append( ">\n" );
        }
      }
      indent( target, indent );
      target.append( "</" ).append( name ).append( ">\n" );
    }
    else
    {
      target.append( "/>\n" );
    }
  }

  /**
   * Make a JSON-compatible URL with the arguments from this {@link Bindings}. URL encodes
   * the arguments in UTF-8 and appends them to the list using standard URL query
   * delimiters.
   * <p/>
   * If an argument is a {@link Bindings} or a {@link List}, it is transformed to JSON.
   * Otherwise, the argument is coerced to a String and URL encoded.
   *
   * @param url The base URL to extend with encoded arguments from this {@link Bindings}
   *
   * @return The URL with JSON-encoded arguments from this {@link Bindings}
   */
  public static URL makeUrl( @This Bindings thiz, String url )
  {
    return ManUrlExt.makeUrl( url, thiz );
  }

  /**
   * Use <a href="https://www.w3.org/MarkUp/html-spec/html-spec_8.html#SEC8.2.3">HTTP POST</a> to pass JSON bindings to
   * this URL and get the full response as a JSON object.
   *
   * @return The full content of this URL's stream as a JSON object.
   *
   * @see ManUrlExt#postForTextContent(URL, Bindings)
   */
  @SuppressWarnings("unused")
  public static Bindings postForJsonContent( @This Bindings thiz, String url )
  {
    try
    {
      return Json.fromJson( ManUrlExt.postForTextContent( new URL( url ), thiz ) );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Get the content from the specified {@code url} using this {@link Bindings} as <a href="https://www.json.org/">JSON-formatted</a> arguments.
   * Encodes the arguments in UTF-8 and appends them to the list using standard <a href="https://www.w3.org/MarkUp/html-spec/html-spec_8.html#SEC8.2.2">HTTP GET format</a>.
   * <p/>
   * This Bindings instance must be composed of property names mapped to JSON values.  A JSON value is one of:
   * <ul>
   *   <li> a simple type such as a String, number, or boolean </li>
   *   <li> a {@link Bindings} of property names to JSON values </li>
   *   <li> a {@link List} composed of JSON values </li>
   * </ul>
   * If an argument is a {@link Bindings} or a {@link List}, it is transformed to JSON.  Otherwise, the argument is
   * coerced to a String and URL encoded.
   *
   * @return JSON bindings reflecting the content of the URL.  Otherwise if the content is not JSON formatted, a
   * {@link RuntimeException} results.
   *
   * @see #postForJsonContent(Bindings, String)
   * @see manifold.api.json.Json#fromJson(String)
   */
  public static Bindings getJsonContent( @This Bindings thiz, String url )
  {
    return Json.fromJson( ManUrlExt.getTextContent( makeUrl( thiz, url ) ) );
  }

  /**
   * Convert this Json {@link Bindings} to an arguments String suitable for a JSON-compatible URL.
   *
   * @return A String formatted with JSON-compatible URL arguments
   */
  public static String makeArguments( @This Bindings arguments )
  {
    try
    {
      StringBuilder sb = new StringBuilder();
      for( Map.Entry<String, Object> entry: arguments.entrySet() )
      {
        if( sb.length() != 0 )
        {
          sb.append( '&' );
        }
        sb.append( URLEncoder.encode( entry.getKey(), "UTF-8" ) )
          .append( '=' )
          .append( makeValue( entry.getValue() ) );
      }
      return sb.toString();
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Convert {@code value} to a String suitable for a JSON URL argument.
   *
   * @param value A JSON value.  One of:
   *              <ul>
   *              <li> a simple type such as a String, number, or boolean </li>
   *              <li> a {@link Bindings} of property names to JSON values </li>
   *              <li> a {@link List} composed of JSON values </li>
   *              </ul>
   *
   * @return JSON formatted value String escaped for URL use.
   */
  @Extension
  public static String makeValue( Object value )
  {
    if( value instanceof Bindings )
    {
      value = JsonUtil.toJson( (Bindings)value );
    }
    else if( value instanceof List )
    {
      value = JsonUtil.listToJson( (List)value );
    }

    try
    {
      return URLEncoder.encode( value.toString(), "UTF-8" );
    }
    catch( UnsupportedEncodingException e )
    {
      throw new RuntimeException( e );
    }
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
}
