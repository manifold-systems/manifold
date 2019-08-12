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

package manifold.json.extensions.java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import manifold.api.json.Yaml;
import manifold.ext.api.Jailbreak;
import manifold.json.extensions.javax.script.Bindings.ManBindingsExt;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.Json;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.api.util.JsonUtil;
import manifold.api.util.StreamUtil;

/**
 * Adds extension methods to URL for handling JSON, plain text, and binary content.
 */
@Extension
public class ManUrlExt
{
  static
  {
    @Jailbreak HttpURLConnection cls = null;
    // add support for "PATCH"
    cls.methods = new String[] {"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH"};
  }

  /**
   * Make a JSON-compatible URL with the arguments from the {@code jsonValue}. Encodes
   * the arguments in UTF-8 and appends them to the list using standard URL query delimiters.
   * <p/>
   * If an individual argument is a {@link Bindings} or a {@link List}, it is transformed to JSON.
   * Otherwise, the argument is coerced to a String and URL encoded.
   */
  public static URL makeUrl( String url, Object jsonValue )
  {
    StringBuilder sb = new StringBuilder();
    if( jsonValue instanceof Bindings )
    {
      for( Map.Entry entry: ((Bindings)jsonValue).entrySet() )
      {
        sb.append( sb.length() == 0 ? '?' : '&' )
          .append( entry.getKey() )
          .append( '=' );
        Object value = entry.getValue();
        value = valueToJson( value );
        sb.append( value );
      }
    }
    else if( jsonValue instanceof List )
    {
      for( Object value: (List)jsonValue )
      {
        sb.append( sb.length() == 0 ? '?' : '&' );
        value = valueToJson( value );
        sb.append( value );
      }
    }
    else if( jsonValue != null )
    {
      String value = encode( String.valueOf( jsonValue ) );
      sb.append( '?' ).append( value );
    }

    try
    {
      return new URL( url + sb );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  private static Object valueToJson( Object value )
  {
    if( value instanceof Bindings )
    {
      value = ManBindingsExt.toJson( ((Bindings)value) );
    }
    else if( value instanceof List )
    {
      value = ManBindingsExt.listToJson( (List)value );
    }
    return encode( String.valueOf( value ) );
  }

  /**
   * Convenience method to encode a URL string and not have to handle the
   * UnsupportedEncodingException.
   */
  @Extension
  public static String encode( String urlPart )
  {
    try
    {
      return URLEncoder.encode( urlPart, "UTF-8" );
    }
    catch( UnsupportedEncodingException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, PATCH, or DELETE to send JSON bindings to a URL and return the raw response body as a
   * String.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", "PATCH", or "DELETE"
   * @param jsonValue A JSON value (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   * @param headers Optional header name/value pairs
   * @param timeout Connection timeout, zero is interpreted as an infinite timeout, a negative value indicates default
   *
   * @param headers
   * @return The raw response as a String.
   *
   * @see #sendJsonRequest(URL, String, Object)
   * @see #sendYamlRequest(URL, String, Object)
   */
  private static String sendRequest( URL url, String httpMethod, Object jsonValue,
                                     Map<String, String> headers, int timeout )
  {
    try
    {
      if( jsonValue != null && (httpMethod.equals( "GET") || httpMethod.equals( "DELETE" )) )
      {
        url = makeUrl( url.toString(), jsonValue );
      }
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod( httpMethod );
      conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
      headers.forEach( conn::setRequestProperty );
      conn.setConnectTimeout( timeout );
      if( jsonValue != null &&
          !httpMethod.equals( "GET" ) && !httpMethod.equals( "DELETE" ) )
      {
        sendJsonValue( jsonValue, conn );
      }
      return receiveResponse( conn );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private static void sendJsonValue( Object jsonValue, HttpURLConnection conn ) throws IOException
  {
    String json = JsonUtil.toJson( jsonValue );
    byte[] bytes = json.getBytes( StandardCharsets.UTF_8 );
    conn.setFixedLengthStreamingMode( bytes.length );
    conn.setDoOutput( true );
    try( OutputStream out = conn.getOutputStream() )
    {
      out.write( bytes );
    }
  }

  private static String receiveResponse( HttpURLConnection conn ) throws IOException
  {
    try( Reader in = StreamUtil.getInputStreamReader( conn.getInputStream() ) )
    {
      return StreamUtil.getContent( in );
    }
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to a URL with a JSON response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return A JSON value parsed from the JSON response.
   *
   * @see #sendRequest(URL, String, Object, Map, int)
   */
  @SuppressWarnings("unused")
  public static Object sendJsonRequest( @This URL url, String httpMethod, Object jsonValue )
  {
    return sendJsonRequest( url, httpMethod, jsonValue, Collections.emptyMap(), 0 );
  }
  public static Object sendJsonRequest( @This URL url, String httpMethod, Object jsonValue,
                                        Map<String, String> headers, int timeout )
  {
    return Json.fromJson( sendRequest( url, httpMethod, jsonValue, headers, timeout ) );
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to a URL with a YAML response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return A JSON value parsed from the YAML response.
   *
   * @see #sendRequest(URL, String, Object, Map, int)
   */
  @SuppressWarnings("unused")
  public static Object sendYamlRequest( @This URL url, String httpMethod, Object jsonValue )
  {
    return sendYamlRequest( url, httpMethod, jsonValue, Collections.emptyMap(), 0 );
  }
  public static Object sendYamlRequest( @This URL url, String httpMethod, Object jsonValue,
                                        Map<String, String> headers, int timeout )
  {
    return Yaml.fromYaml( sendRequest( url, httpMethod, jsonValue, headers, timeout ) );
  }

  /**
   * Use HTTP GET, POST, PUT, or PATCH to send JSON bindings to a URL with a YAML response.
   *
   * @param httpMethod The HTTP method to use: "GET", "POST", "PUT", or "PATCH"
   * @param jsonValue A JSON value to send (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @return The raw response body as plain text
   *
   * @see #sendRequest(URL, String, Object, Map, int)
   */
  @SuppressWarnings("unused")
  public static String sendPlainTextRequest( @This URL url, String httpMethod, Object jsonValue )
  {
    return sendPlainTextRequest( url, httpMethod, jsonValue, Collections.emptyMap(), 0 );
  }
  public static String sendPlainTextRequest( @This URL url, String httpMethod, Object jsonValue,
                                             Map<String, String> headers, int timeout )
  {
    return sendRequest( url, httpMethod, jsonValue, headers, timeout );
  }

  /**
   * @return The full text content of this URL's stream.
   */
  public static String getTextContent( @This URL thiz )
  {
    try( Reader reader = StreamUtil.getInputStreamReader( thiz.openStream() ) )
    {
      return StreamUtil.getContent( reader );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @return The full binary content of this URL's stream.
   */
  public static byte[] getBinaryContent( @This URL thiz )
  {
    try( InputStream stream = thiz.openStream() )
    {
      return StreamUtil.getContent( stream );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @return A JSON object reflecting the contents of this URL, otherwise a {@link RuntimeException} results if the
   * content is not a JSON document.
   *
   * @see manifold.api.json.Json#fromJson(String)
   */
  public static Object getJsonContent( @This URL thiz )
  {
    return Json.fromJson( getTextContent( thiz ) );
  }

  /**
   * @return A YAML object reflecting the contents of this URL, otherwise a {@link RuntimeException} results if the
   * content is not a YAML document.
   *
   * @see manifold.api.json.Yaml#fromYaml(String)
   */
  public static Object getYamlContent( @This URL thiz )
  {
    return Yaml.fromYaml( getTextContent( thiz ) );
  }
}
