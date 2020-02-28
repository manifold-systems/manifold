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

package manifold.graphql.request;

import java.util.List;

/**
 * Thrown when a GraphQL request response contains errors. The errors wrap the {@link graphql.GraphQLError}
 * values in the response as a list of {@link GqlError}.
 */
public class GqlRequestException extends RuntimeException
{
  private final List<GqlError> _errors;

  public GqlRequestException( List<GqlError> errors )
  {
    super( errors.size() == 1 ? errors.get( 0 ).getMessage() : "GraphQL request errors found" );
    _errors = errors;
  }

  public List<GqlError> getErrors()
  {
    return _errors;
  }
}
