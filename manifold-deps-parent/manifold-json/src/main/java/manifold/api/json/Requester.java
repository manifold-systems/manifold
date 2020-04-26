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

package manifold.api.json;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.json.extensions.java.net.URL.ManUrlExt;

/**
 * This class defines methods to simplify making HTTP requests involved with basic REST API calls supporting GET,
 * POST, PUT, PATCH, and DELETE and handles responses in JSON & JSON Schema, YAML, XML, CSV, or plain text.  An instance
 * of this class may be used for multiple get/post/etc. requests.
 * <p/>
 * Normally you use this class via the JSON API {@code request(url)} method to manage simple HTTP request API calls:
 * <pre><code>
 * User user = User.request("http://example.com/users").getOne("/$id");
 *
 * // or
 *
 * Requester&lt;User&gt; req = User.request("http://example.com/users")
 *   .withBearerAuthorization("xxx...x"); // eg., using OAuth token
 * User user = req.getOne("/$id");
 * user.setName("Scott");
 * req.putOne("/$id", user);
 * </code></pre>
 * Note this class is intended for <i>basic</i> REST API use and is designed to supplement more capable REST API
 * frameworks such as Spring.
 *
 * @param <T> The type corresponding with the HTTP requests made from this class.  For instance, this type is returned
 *            from GET calls and is also the type of the payload sent for POST, PUT, and PATCH calls. Since DELETE calls
 *            do not necessarily send or receive this type, it is not part of the signatures of those methods.
 */
public class Requester<T>
{
  private final String _urlBase;
  private Format _format;
  private Map<String, String> _headers;
  private Map<String, String> _parameters;
  private int _timeout;

  public enum Format
  {
    Json, Yaml, Xml, Csv, Text
  }

  /**
   * Get an instance of {@code Requester} from a JSON API type eg., {@code User.request()}.  Requester is a builder
   * type: you can configure the requests you'll make using {@code withXxx()} calls to specify an authorization
   * token, response format, custom headers, etc. Then you can make one or more requests with a single instance:
   * <pre><code>
   * Requester&lt;User&gt; req = User.request("http://example.com/users")
   *   .withBearerAuthorization("xxx...x"); // eg., using OAuth token
   * User user = req.getOne("/$id");
   * user.setName("Scott");
   * req.putOne("/$id", user);
   * </code></pre>
   * @param urlBase A URL providing HTTP services for {@code T}, such as "http://example.com/users"
   */
  public Requester( String urlBase )
  {
    _urlBase = urlBase;
    _format = Format.Json;
    _headers = new HashMap<>();
    _parameters = Collections.emptyMap();
    _timeout = 0;
  }

  /**
   * Set the default format expected in the response. The response will be parsed according to this setting.
   * @param format Json, Yaml, Xml, Csv, or Plain text. Default is Json.
   */
  public Requester<T> withResponseFormat( Format format )
  {
    _format = format;
    return this;
  }

  /**
   * Set an HTTP request header {@code name : value} pair
   * See <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields>HTTP header fields</a>
   */
  public Requester<T> withHeader( String name, String value )
  {
    _headers.put( name, value );
    return this;
  }

  /**
   * Add a {@code name=value} parameter to the request URL.
   */
  public Requester<T> withParam( String name, String value )
  {
    if( _parameters.isEmpty() )
    {
      _parameters = new HashMap<>( 2 );
    }
    _parameters.put( name, value );
    return this;
  }

  /**
   * Set the Basic Authorization header using the provided {@code username} and {@code password}
   */
  @SuppressWarnings("unused")
  public Requester<T> withBasicAuthorization( String username, String password )
  {
    String authorization = Base64.getEncoder()
      .encodeToString(( "$username:$password" ).getBytes( StandardCharsets.UTF_8 ) );
    return withHeader( "Authorization", "Basic $authorization" );
  }

  /**
   * Set the Bearer Authorization header using the provided {@code accessToken}.
   * For instance, if using OAuth, {@code accessToken} is the token response from:
   * <pre><code>
   * curl -d "grant_type=password&client_id=[...]&client_secret=[...]&username=[...]&password=[...]"
   *   https://[domain]/[oauth-service]
   * </code></pre>
   */
  @SuppressWarnings("unused")
  public Requester<T> withBearerAuthorization( String accessToken )
  {
    return withAuthorization( "Bearer", accessToken );
  }
  @SuppressWarnings("unused")
  public Requester<T> withAuthorization( String tokenType, String accessToken )
  {
    return withHeader( "Authorization", "$tokenType $accessToken" );
  }

  /**
   * The connection timeout setting in milliseconds. If the timeout expires before the connection can be established, a
   * {@link java.net.SocketTimeoutException) is thrown. A value of zero is interpreted as an infinite timeout, this is
   * the default setting.
   */
  public Requester<T> withTimeout( int timeout )
  {
    _timeout = timeout;
    return this;
  }

  /**
   * Use HTTP GET for a single {@code T} JSON API object specified in the {@code urlSuffix}, such as {@code "/108"}.
   *
   * @return A single {@code T} JSON API object specified in the {@code urlSuffix}
   * <p/>
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne("", null, _format)}
   */
  public T getOne()
  {
    return getOne( "", null );
  }

  /**
   * Use HTTP GET for a single {@code T} JSON API object specified in the {@code urlSuffix}, such as {@code "/108"}.
   *
   * @param urlSuffix A suffix identifying the {@code T} JSON API object to getOne
   *
   * @return A single {@code T} JSON API object specified in the {@code urlSuffix}
   * <p/>
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne(urlSuffix, null, _format)}
   */
  public T getOne( String urlSuffix )
  {
    return getOne( urlSuffix, null );
  }

  /**
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne("", arguments, _format)}
   */
  public T getOne( Object arguments )
  {
    return getOne( "", arguments );
  }

  /**
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne(urlSuffix, arguments, _format)}
   */
  public T getOne( String urlSuffix, Object arguments )
  {
    return getOne( urlSuffix, arguments, _format );
  }

  /**
   * Make an HTTP GET request to {@code urlBase + urlSuffix}.  {@code arguments}, if non-null, is sent in the URL as
   * JSON encoded URL arguments.
   *
   * @param arguments A JSON value object, sent in the URL as JSON encoded arguments, nullable
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or
   * Bindings of String/JSON value)
   */
  public T getOne( String urlSuffix, Object arguments, Format format )
  {
    return request( urlSuffix, Http.GET, format, arguments );
  }

  /**
   * Uses HTTP GET for the complete list of {@code T} JSON API objects as a {@code IJsonList<T>}.
   *
   * @return The complete list of {@code T} JSON API objects as a {@code IJsonList<T>}
   * <p/>
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany("", null, _format)}
   */
  public IJsonList<T> getMany()
  {
    return getMany( "", null );
  }

  /**
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany(urlSuffix, null, _format)}
   */
  public IJsonList<T> getMany( String urlSuffix )
  {
    return getMany( urlSuffix, null );
  }

  /**
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany("", arguments, _format)}
   */
  public IJsonList<T> getMany( Object arguments )
  {
    return getMany( "", arguments );
  }

  /**
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany(urlSuffix, arguments, _format)}
   */
  public IJsonList<T> getMany( String urlSuffix, Object arguments )
  {
    return getMany( urlSuffix, arguments, _format );
  }

  /**
   * Make an HTTP GET request to {@code urlBase + urlSuffix}.  {@code arguments}, if non-null, is sent in the URL as
   * JSON encoded URL arguments.
   *
   * @param arguments A JSON value object, sent in the URL as JSON encoded arguments, nullable
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or
   * Bindings of String/JSON value)
   */
  public IJsonList<T> getMany( String urlSuffix, Object arguments, Format format )
  {
    return request( urlSuffix, Http.GET, format, arguments );
  }

  /**
   * Same as calling:
   * {@link #postOne(String, Object, Format)} with {@code postOne("", payload, _format)}
   */
  public <R> R postOne( T payload )
  {
    return postOne( "", payload );
  }

  /**
   * Same as calling:
   * {@link #postOne(String, Object, Format)} with {@code postOne(urlSuffix, payload, _format)}
   */
  public <R> R postOne( String urlSuffix, T payload )
  {
    return postOne( urlSuffix, payload, _format );
  }

  /**
   * Make an HTTP POST request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R postOne( String urlSuffix, T payload, Format format )
  {
    return request( urlSuffix, Http.POST, format, payload );
  }

  /**
   * Same as calling:
   * {@link #postMany(String, List, Format)} with {@code postMany("", payload, _format)}
   */
  public <R> R postMany( List<T> payload )
  {
    return postMany( "", payload );
  }

  /**
   * Same as calling:
   * {@link #postMany(String, List, Format)} with {@code postMany(urlSuffix, payload, _format)}
   */
  public <R> R postMany( String urlSuffix, List<T> payload )
  {
    return postMany( urlSuffix, payload, _format );
  }

  /**
   * Make an HTTP POST request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R postMany( String urlSuffix, List<T> payload, Format format )
  {
    return request( urlSuffix, Http.POST, format, payload );
  }

  /**
   * Same as calling:
   * {@link #putOne(String, Object, Format)} with {@code putOne("", payload, _format)}
   */
  public <R> R putOne( T payload )
  {
    return putOne( "", payload );
  }

  /**
   * Same as calling:
   * {@link #putOne(String, Object, Format)} with {@code putOne(urlSuffix, payload, _format)}
   */
  public <R> R putOne( String urlSuffix, T payload )
  {
    return putOne( urlSuffix, payload, _format );
  }

  /**
   * Make an HTTP PUT request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R putOne( String urlSuffix, T payload, Format format )
  {
    return request( urlSuffix, Http.PUT, format, payload );
  }

  /**
   * Same as calling:
   * {@link #putMany(String, List, Format)} with {@code putMany("", payload, _format)}
   */
  public <R> R putMany( List<T> payload )
  {
    return putMany( "", payload );
  }

  /**
   * Same as calling:
   * {@link #putMany(String, List, Format)} with {@code putMany(urlSuffix, payload, _format)}
   */
  public <R> R putMany( String urlSuffix, List<T> payload )
  {
    return putMany( urlSuffix, payload, _format );
  }

  /**
   * Make an HTTP PUT request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R putMany( String urlSuffix, List<T> payload, Format format )
  {
    return request( urlSuffix, Http.PUT, format, payload );
  }

  /**
   * Same as calling:
   * {@link #patchOne(String, Object, Format)} with {@code patchOne("", payload, _format)}
   */
  public <R> R patchOne( T payload )
  {
    return patchOne( "", payload );
  }

  /**
   * Same as calling:
   * {@link #patchOne(String, Object, Format)} with {@code patchOne(urlSuffix, payload, _format)}
   */
  public <R> R patchOne( String urlSuffix, T payload )
  {
    return patchOne( urlSuffix, payload, _format );
  }

  /**
   * Make an HTTP PATCH request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R patchOne( String urlSuffix, T payload, Format format )
  {
    return request( urlSuffix, Http.PATCH, format, payload );
  }

  /**
   * Same as calling:
   * {@link #patchMany(String, List, Format)} with {@code patchMany("", payload, _format)}
   */
  public <R> R patchMany( List<T> payload )
  {
    return patchMany( "", payload );
  }

  /**
   * Same as calling:
   * {@link #patchMany(String, List, Format)} with {@code patchMany(urlSuffix, payload, _format)}
   */
  public <R> R patchMany( String urlSuffix, List<T> payload )
  {
    return patchMany( urlSuffix, payload, _format );
  }

  /**
   * Make an HTTP PATCH request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R patchMany( String urlSuffix, List<T> payload, Format format )
  {
    return request( urlSuffix, Http.PATCH, format, payload );
  }

  /**
   * Same as calling:
   * {@link #delete(String, Object, Format)} with {@code delete("", arguments, _format)}
   */
  public <R> R delete( Object arguments )
  {
    return delete( "", arguments );
  }

  /**
   * Same as calling:
   * {@link #delete(String, Object, Format)} with {@code delete(urlSuffix, null, _format)}
   */
  public <R> R delete( String urlSuffix )
  {
    return delete( urlSuffix, null );
  }

  /**
   * Same as calling:
   * {@link #delete(String, Object, Format)} with {@code delete(urlSuffix, arguments, _format)}
   */
  public <R> R delete( String urlSuffix, Object arguments )
  {
    return delete( urlSuffix, arguments, _format );
  }

  /**
   * Make an HTTP DELETE request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent in the URL as JSON
   * encoded URL arguments.
   *
   * @param <R>       The expected type of the response
   * @param arguments A JSON value object, sent in the URL as JSON encoded arguments, nullable
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, {@code Xml}, {@code Csv}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R delete( String urlSuffix, Object arguments, Format format )
  {
    return request( urlSuffix, Http.DELETE, format, arguments );
  }


  private <R> R request( String urlSuffix, Http method, Format format, Object jsonValue )
  {
    urlSuffix = appendParams( urlSuffix );
    switch( format )
    {
      case Json:
        _headers.put( "Accept", "application/json" );
        return Request.send( ( url, p, m ) -> ManUrlExt.sendJsonRequest( url, m, jsonValue, _headers, _timeout ),
          method, jsonValue, _urlBase, urlSuffix );
      case Yaml:
        _headers.put( "Accept", "application/x-yaml, application/yaml, text/yaml;q=0.9" );
        return Request.send( ( url, p, m ) -> ManUrlExt.sendYamlRequest( url, m, jsonValue, _headers, _timeout ),
          method, jsonValue, _urlBase, urlSuffix );
      case Xml:
        _headers.put( "Accept", "application/xml" );
        return Request.send( ( url, p, m ) -> ManUrlExt.sendXmlRequest( url, m, jsonValue, _headers, _timeout ),
          method, jsonValue, _urlBase, urlSuffix );
      case Csv:
        _headers.put( "Accept", "text/csv" );
        return Request.send( ( url, p, m ) -> ManUrlExt.sendCsvRequest( url, m, jsonValue, _headers, _timeout ),
          method, jsonValue, _urlBase, urlSuffix );
      case Text:
        return Request.send( ( url, p, m ) -> ManUrlExt.sendPlainTextRequest( url, m, jsonValue, _headers, _timeout ),
          method, jsonValue, _urlBase, urlSuffix );
    }
    throw new IllegalArgumentException( "format: " + format );
  }

  private String appendParams( String urlSuffix )
  {
    if( _parameters.isEmpty() )
    {
      return urlSuffix;
    }

    boolean firstParam = urlSuffix.indexOf( '?' ) < 0;
    StringBuilder sb = new StringBuilder( urlSuffix );
    for( Map.Entry<String, String> entry: _parameters.entrySet() )
    {
      sb.append( firstParam ? '?' : '&' )
        .append( entry.getKey() )
        .append( '=' )
        .append( entry.getValue() );
      firstParam = false;
    }
    return sb.toString();
  }

  @FunctionalInterface
  private interface Request
  {
    Object send( URL url, Object payload, String method );

    static <R> R send( Request sender, Http method, Object jsonValue, String urlBase, String urlSuffix )
    {
      if( urlSuffix != null )
      {
        urlBase += urlSuffix;
      }

      try
      {
        //noinspection unchecked
        return (R)sender.send( new URL( urlBase ), jsonValue, method.name() );
      }
      catch( MalformedURLException e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  private enum Http
  {
    GET, POST, PUT, PATCH, DELETE
  }
}
