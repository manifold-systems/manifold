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
import java.util.List;
import manifold.json.extensions.java.net.URL.ManUrlExt;

/**
 * This class defines methods to simplify making HTTP requests involved with basic REST API calls supporting via GET,
 * POST, PUT, PATCH, and DELETE and handles responses in JSON/JSON Schema, YAML, XML, or plain text.
 * <p/>
 * Normally you use this class via the JSON API {@code request(url)} method to manage simple HTTP request API calls:
 * <pre><code>  User user = User.request("http://example.com/users").getOne("/$id");</code></pre>
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

  public enum Format
  {
    Json, Yaml, Plain
  }

  /**
   * @param urlBase A URL providing HTTP services for {@code T}, such as "http://example.com/users"
   */
  public Requester( String urlBase )
  {
    _urlBase = urlBase;
  }

  /**
   * Use HTTP GET for a single {@code T} JSON API object specified in the {@code urlSuffix}, such as {@code "/108"}.
   *
   * @return A single {@code T} JSON API object specified in the {@code urlSuffix}
   * <p/>
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne("", null, Format.Json)}
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
   * {@link #getOne(String, Object, Format)} with {@code getOne(urlSuffix, null, Format.Json)}
   */
  public T getOne( String urlSuffix )
  {
    return getOne( urlSuffix, null );
  }

  /**
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne("", arguments, Format.Json)}
   */
  public T getOne( Object arguments )
  {
    return getOne( "", arguments );
  }

  /**
   * Same as calling:
   * {@link #getOne(String, Object, Format)} with {@code getOne(urlSuffix, arguments, Format.Json)}
   */
  public T getOne( String urlSuffix, Object arguments )
  {
    return getOne( urlSuffix, arguments, Format.Json );
  }

  /**
   * Make an HTTP GET request to {@code urlBase + urlSuffix}.  {@code arguments}, if non-null, is sent in the URL as
   * JSON encoded URL arguments.
   *
   * @param arguments A JSON value object, sent in the URL as JSON encoded arguments, nullable
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
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
   * {@link #getMany(String, Object, Format)} with {@code getMany("", null, Format.Json)}
   */
  public IJsonList<T> getMany()
  {
    return getMany( "", null );
  }

  /**
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany(urlSuffix, null, Format.Json)}
   */
  public IJsonList<T> getMany( String urlSuffix )
  {
    return getMany( urlSuffix, null );
  }

  /**
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany("", arguments, Format.Json)}
   */
  public IJsonList<T> getMany( Object arguments )
  {
    return getMany( "", arguments );
  }

  /**
   * Same as calling:
   * {@link #getMany(String, Object, Format)} with {@code getMany(urlSuffix, arguments, Format.Json)}
   */
  public IJsonList<T> getMany( String urlSuffix, Object arguments )
  {
    return getMany( urlSuffix, arguments, Format.Json );
  }

  /**
   * Make an HTTP GET request to {@code urlBase + urlSuffix}.  {@code arguments}, if non-null, is sent in the URL as
   * JSON encoded URL arguments.
   *
   * @param arguments A JSON value object, sent in the URL as JSON encoded arguments, nullable
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
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
   * {@link #postOne(String, Object, Format)} with {@code postOne("", payload, Format.Json)}
   */
  public <R> R postOne( T payload )
  {
    return postOne( "", payload );
  }

  /**
   * Same as calling:
   * {@link #postOne(String, Object, Format)} with {@code postOne(urlSuffix, payload, Format.Json)}
   */
  public <R> R postOne( String urlSuffix, T payload )
  {
    return postOne( urlSuffix, payload, Format.Json );
  }

  /**
   * Make an HTTP POST request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R postOne( String urlSuffix, T payload, Format format )
  {
    return request( urlSuffix, Http.POST, format, payload );
  }

  /**
   * Same as calling:
   * {@link #postMany(String, List, Format)} with {@code postMany("", payload, Format.Json)}
   */
  public <R> R postMany( List<T> payload )
  {
    return postMany( "", payload );
  }

  /**
   * Same as calling:
   * {@link #postMany(String, List, Format)} with {@code postMany(urlSuffix, payload, Format.Json)}
   */
  public <R> R postMany( String urlSuffix, List<T> payload )
  {
    return postMany( urlSuffix, payload, Format.Json );
  }

  /**
   * Make an HTTP POST request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R postMany( String urlSuffix, List<T> payload, Format format )
  {
    return request( urlSuffix, Http.POST, format, payload );
  }

  /**
   * Same as calling:
   * {@link #putOne(String, Object, Format)} with {@code putOne("", payload, Format.Json)}
   */
  public <R> R putOne( T payload )
  {
    return putOne( "", payload );
  }

  /**
   * Same as calling:
   * {@link #putOne(String, Object, Format)} with {@code putOne(urlSuffix, payload, Format.Json)}
   */
  public <R> R putOne( String urlSuffix, T payload )
  {
    return putOne( urlSuffix, payload, Format.Json );
  }

  /**
   * Make an HTTP PUT request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R putOne( String urlSuffix, T payload, Format format )
  {
    return request( urlSuffix, Http.PUT, format, payload );
  }

  /**
   * Same as calling:
   * {@link #putMany(String, List, Format)} with {@code putMany("", payload, Format.Json)}
   */
  public <R> R putMany( List<T> payload )
  {
    return putMany( "", payload );
  }

  /**
   * Same as calling:
   * {@link #putMany(String, List, Format)} with {@code putMany(urlSuffix, payload, Format.Json)}
   */
  public <R> R putMany( String urlSuffix, List<T> payload )
  {
    return putMany( urlSuffix, payload, Format.Json );
  }

  /**
   * Make an HTTP PUT request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R putMany( String urlSuffix, List<T> payload, Format format )
  {
    return request( urlSuffix, Http.PUT, format, payload );
  }

  /**
   * Same as calling:
   * {@link #patchOne(String, Object, Format)} with {@code patchOne("", payload, Format.Json)}
   */
  public <R> R patchOne( T payload )
  {
    return patchOne( "", payload );
  }

  /**
   * Same as calling:
   * {@link #patchOne(String, Object, Format)} with {@code patchOne(urlSuffix, payload, Format.Json)}
   */
  public <R> R patchOne( String urlSuffix, T payload )
  {
    return patchOne( urlSuffix, payload, Format.Json );
  }

  /**
   * Make an HTTP PATCH request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R patchOne( String urlSuffix, T payload, Format format )
  {
    return request( urlSuffix, Http.PATCH, format, payload );
  }

  /**
   * Same as calling:
   * {@link #patchMany(String, List, Format)} with {@code patchMany("", payload, Format.Json)}
   */
  public <R> R patchMany( List<T> payload )
  {
    return patchMany( "", payload );
  }

  /**
   * Same as calling:
   * {@link #patchMany(String, List, Format)} with {@code patchMany(urlSuffix, payload, Format.Json)}
   */
  public <R> R patchMany( String urlSuffix, List<T> payload )
  {
    return patchMany( urlSuffix, payload, Format.Json );
  }

  /**
   * Make an HTTP PATCH request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param <R>       The expected type of the response
   * @param payload   A JSON value object, sent as JSON encoded text in the request's message body
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R patchMany( String urlSuffix, List<T> payload, Format format )
  {
    return request( urlSuffix, Http.PATCH, format, payload );
  }

  /**
   * Same as calling:
   * {@link #delete(String, Object, Format)} with {@code delete("", arguments, Format.Json)}
   */
  public <R> R delete( Object arguments )
  {
    return delete( "", arguments );
  }

  /**
   * Same as calling:
   * {@link #delete(String, Object, Format)} with {@code delete(urlSuffix, null, Format.Json)}
   */
  public <R> R delete( String urlSuffix )
  {
    return delete( urlSuffix, null );
  }

  /**
   * Same as calling:
   * {@link #delete(String, Object, Format)} with {@code delete(urlSuffix, arguments, Format.Json)}
   */
  public <R> R delete( String urlSuffix, Object arguments )
  {
    return delete( urlSuffix, arguments, Format.Json );
  }

  /**
   * Make an HTTP DELETE request to {@code urlBase + urlSuffix}.  The {@code payload}, if non-null, is sent in the URL as JSON
   * encoded URL arguments.
   *
   * @param <R>       The expected type of the response
   * @param arguments A JSON value object, sent in the URL as JSON encoded arguments, nullable
   * @param urlSuffix A suffix, such as "/108", nullable
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public <R> R delete( String urlSuffix, Object arguments, Format format )
  {
    return request( urlSuffix, Http.DELETE, format, arguments );
  }


  private <R> R request( String urlSuffix, Http method, Format format, Object jsonValue )
  {
    switch( format )
    {
      case Json:
        return Request.send( ( url, p, m ) -> ManUrlExt.sendJsonRequest( url, m, jsonValue ),
          method, jsonValue, _urlBase, urlSuffix );
      case Yaml:
        return Request.send( ( url, p, m ) -> ManUrlExt.sendYamlRequest( url, m, jsonValue ),
          method, jsonValue, _urlBase, urlSuffix );
      case Plain:
        return Request.send( ( url, p, m ) -> ManUrlExt.sendPlainTextRequest( url, m, jsonValue ),
          method, jsonValue, _urlBase, urlSuffix );
    }
    throw new IllegalArgumentException( "format: " + format );
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
