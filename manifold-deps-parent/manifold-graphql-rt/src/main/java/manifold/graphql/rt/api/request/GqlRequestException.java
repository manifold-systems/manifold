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

import manifold.json.rt.api.JsonList;
import manifold.rt.api.Bindings;

import java.util.List;

/**
 * Thrown when a GraphQL request response contains errors. The errors wrap the {@link graphql.GraphQLError}
 * values in the response as a type-safe list of {@link GqlError} from {@link #getErrors()}. The query result data,
 * if available, is also type-safely accessible from {@link #getResult(Class)}.
 */
public class GqlRequestException extends RuntimeException
{
  private final Bindings _response;

  public GqlRequestException( Bindings response )
  {
    _response = response;
  }

  public <E> E getResult( Class<E> resultType )
  {
    //noinspection unchecked
    return (E)_response.get( "data" );
  }

  @Override
  public String getMessage()
  {
    //noinspection unchecked
    List<GqlError> errors = (List)_response.get( "errors" );
    return errors.size() == 1 ? errors.get( 0 ).getMessage() : "GraphQL request errors found";
  }

  public List<GqlError> getErrors()
  {
    //noinspection unchecked
    List<GqlError> errors = (List)_response.get( "errors" );
    return new JsonList<>( errors, GqlError.class );
  }
}
