/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.graphql.rt.api.request;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import manifold.ext.rt.RuntimeMethods;
import manifold.rt.api.Bindings;

import manifold.json.rt.api.Endpoint;
import manifold.json.rt.api.Requester;

/**
 * Based on: "How to make GraphQL HTTP request using cUrl"
 * <p/>
 * Based on the GET/POST and the Content-Type header, it expects the input params differently.
 * This behaviour was ported from express-graphql.
 * <p/>
 * So given the following operation:
 * <pre><code>
 * mutation M {
 *   newTodo: createTodo(text: "This is a mutation example") {
 *     text
 *     done
 *   }
 * }
 * </code></pre>
 * <pre>
 * using GET
 * $ curl -g -GET 'http://localhost:8080/graphql?query=mutation+M{newTodo:createTodo(text:"This+is+a+mutation+example"){text+done}}'
 * using POST + Content-Type: application/graphql
 * $ curl -XPOST http://localhost:8080/graphql -H 'Content-Type: application/graphql' -d 'mutation M { newTodo: createTodo(text: "This is a mutation example") { text done } }'
 * using POST + Content-Type: application/json
 * $ curl -XPOST http://localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query": "mutation M { newTodo: createTodo(text: \"This is a mutation example\") { text done } }"}'
 * </pre>
 * @param <T>
 */
public class Executor<T>
{
  private final GqlRequestBody _reqArgs;
  private final Requester<Bindings> _requester;

  public Executor( String url, String operation, String query, Bindings variables, Class<T> resultType )
  {
    _requester = new Requester<>( url, result -> coerce( (Class<T>) resultType, result ) );
    _requester.withHeader( "Content-Type", "application/json" );
    _reqArgs = GqlRequestBody.create( query, variables );
  }

  public Executor( Endpoint endpoint, String operation, String query, Bindings variables, Class<T> resultType )
  {
    _requester = new Requester<>( endpoint, result -> coerce( resultType, result ) );
    _requester.withHeader( "Content-Type", "application/json" );
    _reqArgs = GqlRequestBody.create( query, variables );
  }

  public Executor( Supplier<Requester<Bindings>> requester, String operation, String query, Bindings variables, Class<T> resultType )
  {
    _requester = requester.get();
    _requester.withCoercer( result -> coerce( resultType, result ) );
    _requester.withHeader( "Content-Type", "application/json" );
    _reqArgs = GqlRequestBody.create( query, variables );
  }

  private Object coerce( Class<T> resultType, Object result )
  {
    Bindings response = (Bindings) result;
    Object customResult = handleRawResponse( response );
    if( customResult != null )
    {
      // a custom result bindings
      return customResult;
    }
    handleErrors( response );
    return RuntimeMethods.coerce( response.get( "data" ), resultType );
  }

  /**
   * Access the full GraphQL request body, which includes {@code query} and {@code variables} bindings.
   *
   * @return the GraphQL request body consisting of bindings for both the query and variables.
   */
  public GqlRequestBody getRequestBody()
  {
    return _reqArgs;
  }

  /**
   * Access an unmodifiable view of the GraphQL request headers.
   */
  public Map<String, String> getHeaders()
  {
    return _requester.getHeaders();
  }

  /**
   * Access the GraphQL request endpoint.
   */
  public Endpoint getEndpoint()
  {
    return _requester.getEndpoint();
  }

  /**
   * Access the GraphQL request format.
   */
  public Requester.Format getFormat()
  {
    return _requester.getFormat();
  }

  /**
   * Access the GraphqL request timeout.
   */
  public int getTimeout()
  {
    return _requester.getTimeout();
  }

  /**
   * Set an HTTP request header {@code name : value} pair
   * See <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">HTTP header fields</a>
   */
  public Executor<T> withHeader( String name, String value )
  {
    _requester.withHeader( name, value );
    return this;
  }

  /**
   * Add a {@code name=value} parameter to the request URL.
   */
  public Executor<T> withParam( String name, String value )
  {
    _requester.withParam( name, value );
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
    return withHeader( "Authorization", tokenType + " " + accessToken );
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
   * @param handler An optional handler for processing the raw response as an arbitrary Bindings instance. The handler
   *                may return a custom bindings object which overrides the default, type-safe result instance. In any
   *                case, the handler can process the response in any way. Note, modifications made to the response
   *                persist and, therefore, affect default internal data and error processing.
   * @return this {@code Executor} instance.
   */
  public Executor<T> withRawResponseHandler( Function<Bindings, Object> handler )
  {
    _requester.withRawResponseHandler( handler );
    return this;
  }

  public Function<Bindings, Object> getRawResponseHandler()
  {
    return _requester.getRawResponseHandler();
  }

  /**
   * Make an HTTP POST request to {@code url}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @throws GqlRequestException If the response contains errors, wraps them in a list of {@link GqlError} and throws
   */
  public T post() throws GqlRequestException
  {
    return _requester.postOne( _reqArgs.getBindings() );
  }

  /**
   * Make an HTTP POST request to {@code url}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @throws GqlRequestException If the response contains errors, wraps them in a list of {@link GqlError} and throws
   */
  public T post( Requester.Format format ) throws GqlRequestException
  {
    return _requester.postOne( "", _reqArgs.getBindings(), format );
  }

  /**
   * Make an HTTP GET request to {@code url}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @throws GqlRequestException If the response contains errors, wraps them in a list of {@link GqlError} and throws
   */
  public T get() throws GqlRequestException
  {
    return (T)_requester.getOne( _reqArgs.getBindings() );
  }

  /**
   * Make an HTTP GET request to {@code url}.  The {@code payload}, if non-null, is sent as JSON encoded
   * text in the request's message body.
   *
   * @param format    The expected format of the response.  One of: {@code Json}, {@code Yaml}, or {@code Plain}
   *
   * @return A JSON value parsed from the {@code format} specified encoded response (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   *
   * @throws GqlRequestException If the response contains errors, wraps them in a list of {@link GqlError} and throws
   */
  public T get( Requester.Format format ) throws GqlRequestException
  {
    return (T)_requester.getOne( "", _reqArgs.getBindings(), format );
  }

  private Object handleRawResponse( Bindings response )
  {
    Function<Bindings, Object> handler = _requester.getRawResponseHandler();
    if( handler != null )
    {
      return handler.apply( response );
    }
    return null;
  }

  private void handleErrors( Bindings response )
  {
    //noinspection unchecked
    List<GqlError> errors = (List)response.get( "errors" );
    if( errors != null && !errors.isEmpty() )
    {
      throw new GqlRequestException( response );
    }
  }
}
