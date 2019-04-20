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

package manifold.graphql.request;

import javax.script.Bindings;
import manifold.api.json.Requester;

public class Executor<T>
{
  private final IGqlRequestArguments _reqArgs;
  private Requester<Bindings> _requester;

  public Executor( String url, String query, Bindings variables )
  {
    _requester = new Requester<>( url );
    _reqArgs = IGqlRequestArguments.create( query, variables );
  }

  /**
   * Set an HTTP request header {@code name : value} pair
   * See <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields>HTTP header fields</a>
   */
  public Executor<T> withHeader( String name, String value )
  {
    _requester.withHeader( name, value );
    return this;
  }

  /**
   * Set the Basic Authorization header using the provided {@code username} and {@code password}
   */
  @SuppressWarnings("unused")
  public Executor<T> withBasicAuthorization( String username, String password )
  {
    _requester.withBasicAuthorization( username, password );
    return this;
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
  public Executor<T> withBearerAuthorization( String accessToken )
  {
    return withAuthorization( "Bearer", accessToken );
  }
  @SuppressWarnings("unused")
  public Executor<T> withAuthorization( String tokenType, String accessToken )
  {
    return withHeader( "Authorization", "$tokenType $accessToken" );
  }

  /**
   * The connection timeout setting in milliseconds. If the timeout expires before the connection can be established, a
   * {@link java.net.SocketTimeoutException) is thrown. A value of zero is interpreted as an infinite timeout, this is
   * the default setting.
   */
  public Executor<T> withTimeout( int timeout )
  {
    _requester.withTimeout( timeout );
    return this;
  }

  /**
   */
  public T post()
  {
    return (T)((Bindings)_requester.postOne( _reqArgs.getBindings() )).get( "data ");
  }

  /**
   * Make an HTTP POST request to {@code url}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public T post( Requester.Format format )
  {
    return (T)((Bindings)_requester.postOne( "", _reqArgs.getBindings(), format )).get( "data" );
  }

  /**
   */
  public T get()
  {
    return (T)((Bindings)_requester.getOne( _reqArgs.getBindings() )).get( "data" );
  }

  /**
   * Make an HTTP GET request to {@code url}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  public T get( Requester.Format format )
  {
    return (T)((Bindings)_requester.getOne( "", _reqArgs.getBindings(), format )).get( "data" );
  }
}
