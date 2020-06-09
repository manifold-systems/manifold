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

package manifold.json.rt.extensions.manifold.rt.api.Bindings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import manifold.rt.api.Bindings;

import manifold.json.rt.Json;
import manifold.json.rt.api.DataBindings;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import manifold.json.rt.api.IJsonBindingsTranslator;
import manifold.json.rt.extensions.java.net.URL.ManUrlExt;
import manifold.rt.api.util.Pair;

/**
 * Extends {@link Bindings} with methods to transform the Bindings contents to JSON, YAML, CSV, and XML and to conveniently
 * use the Bindings for JSON Web services.
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
   * Serializes this {@link Bindings} instance to a YAML formatted String
   *
   * @return This {@link Bindings} instance serialized to a YAML formatted String
   */
  public static String toYaml( @This Bindings thiz )
  {
    StringBuilder sb = new StringBuilder();
    toYaml( thiz, sb );
    return sb.toString();
  }

  /**
   * Serializes this {@link Bindings} instance into a JSON formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param target A {@link StringBuilder} to write the JSON in
   * @param margin The margin of spaces to indent the resulting block of JSON
   */
  public static void toJson( @This Bindings thiz, StringBuilder target, int margin )
  {
    Json.toJson( thiz, target, margin );
  }

  /**
   * Serializes this {@link Bindings} instance into a YAML 1.2 formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param target A {@link StringBuilder} to write the YAML in
   */
  public static void toYaml( @This Bindings thiz, StringBuilder target )
  {
    IJsonBindingsTranslator Yaml = IJsonBindingsTranslator.get( "YAML" );
    Yaml.fromBindings( thiz, target );
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
          Json.appendValue( target, comp );
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
    IJsonBindingsTranslator Xml = IJsonBindingsTranslator.get( "XML" );
    Xml.fromBindings( thiz, name, target, indent );
  }

  /**
   * Serializes this {@link Bindings} instance to CSV nested in a root element named {@code "object"}
   *
   * @see #toCsv(Bindings, String)
   * @see #toCsv(Bindings, String, StringBuilder, int)
   */
  public static String toCsv( @This Bindings thiz )
  {
    return toCsv( thiz, "object" );
  }

  /**
   * Serializes this {@link Bindings} instance to CSV with in a root element with the specified {@code name}
   *
   * @param name The name of the root element to nest the Bindings CSV
   *
   * @see #toCsv(Bindings, String, StringBuilder, int)
   */
  public static String toCsv( @This Bindings thiz, String name )
  {
    StringBuilder sb = new StringBuilder();
    toCsv( thiz, name, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this {@link Bindings} instance into an CSV formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param name   The name of the root element to nest the Bindings CSV
   * @param target A {@link StringBuilder} to write the CSV in
   * @param indent The margin of spaces to indent the CSV
   */
  public static void toCsv( @This Bindings thiz, String name, StringBuilder target, int indent )
  {
    IJsonBindingsTranslator Csv = IJsonBindingsTranslator.get( "CSV" );
    Csv.fromBindings( thiz, name, target, indent );
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
   * Use HTTP GET, POST, PUT, PATCH, or DELETE to send this {@code Bindings} to a URL with a JSON response.
   *
   * @param httpMethod HTTP method to use: "GET", "POST", "PUT", "PATCH", or "DELETE"
   * @param url The URL to send this Bindings to
   * @return The full content of this URL's JSON response as a JSON value.
   *
   * @see ManUrlExt#sendJsonRequest(URL, String, Object)
   */
  @SuppressWarnings("unused")
  public static Object sendJsonRequest( @This Bindings thiz, String httpMethod, String url )
  {
    try
    {
      return ManUrlExt.sendJsonRequest( new URL( url ), httpMethod, thiz );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, PATCH, or DELETE to send this {@code Bindings} to a URL with a YAML response.
   *
   * @param httpMethod HTTP method to use: "GET", "POST", "PUT", "PATCH", or "DELETE"
   * @param url The URL to send this Bindings to
   * @return The full content of this URL's YAML response as a JSON value.
   *
   * @see ManUrlExt#sendYamlRequest(URL, String, Object)
   */
  @SuppressWarnings("unused")
  public static Object sendYamlRequest( @This Bindings thiz, String httpMethod, String url )
  {
    try
    {
      return ManUrlExt.sendYamlRequest( new URL( url ), httpMethod, thiz );
    }
    catch( MalformedURLException e )
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

  /**
   * Provide a deep copy of this {@code Bindings} using a {@code DataBindings} for the copy.
   * <p/>
   * Same as invoking: {@code deepCopy(DataBindings::new)}
   *
   * @return A deep copy of this {@code Bindings}
   */
  public static Bindings deepCopy( @This Bindings thiz )
  {
    return deepCopy( thiz, DataBindings::new );
  }

  /**
   * Provide a deep copy of this {@code Bindings}.  Note this method assumes the Bindings is limited to a JSON
   * style {@code Bindings<String, Value>} where {@code Value} type is strictly:
   * <ul>
   * <li>a {@code String}, {@code Number}, or {@code Boolean}</li>
   * <li>a {@code List} of {@code Value}</li>
   * <li>a {@code Bindings} of {@code String} to {@code Value}</li>
   * </ul>
   * Any deviation from this format may result in unexpected behavior.
   *
   * @param bindingsSupplier Creates the {@code Bindings} instance used for the copy and instances for nested {@code Bindings}.
   *
   * @return A deep copy of this {@code Bindings}
   */
  public static <E extends Bindings> E deepCopy( @This Bindings thiz, Function<Integer, E> bindingsSupplier )
  {
    //noinspection unchecked
    return (E)deepCopyValue( thiz, bindingsSupplier );
  }

  @Extension
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
