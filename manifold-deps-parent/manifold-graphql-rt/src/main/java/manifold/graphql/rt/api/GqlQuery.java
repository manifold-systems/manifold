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

package manifold.graphql.rt.api;

import manifold.ext.rt.api.Structural;
import manifold.graphql.rt.api.request.Executor;
import manifold.json.rt.api.Endpoint;
import manifold.json.rt.api.Requester;
import manifold.rt.api.Bindings;

import java.util.function.Supplier;

/**
 * A base class for all GraphQL queries.
 *
 * @param <R> The query result type mirroring the fields and structure of the query
 */
@Structural
public interface GqlQuery<R extends GqlQueryResult> extends GqlType
{
  /**
   * Provides an HTTP request executor for the query. Use it to configure an HTTP request headers, authorization, etc.
   * and to GET or POST the query.
   *
   * @param url The endpoint of the request as a URL.
   * @return The request executor in terms of the query result type.
   */
  Executor<R> request( String url );

  /**
   * Provides an HTTP request executor for the query. Use it to configure an HTTP request headers, authorization, etc.
   * and to GET or POST the query.
   *
   * @param endpoint The endpoint of the request as proxy-enabled URL.
   * @return The request executor in terms of the query result type.
   */
  Executor<R> request( Endpoint endpoint );

  /**
   * Provides an HTTP request executor for the query. Use it to configure an HTTP request headers, authorization, etc.
   * and to GET or POST the query.
   *
   * @param requester A callback allowing the caller to supply a Requester, typically to the request can be reused
   *                  across multiple query executions.
   * @return The request executor in terms of the query result type.
   */
  Executor<R> request( Supplier<Requester<Bindings>> requester );
}
