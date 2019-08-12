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

package manifold.graphql.type;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import manifold.api.json.schema.FormatTypeResolvers;
import manifold.api.json.schema.IJsonFormatTypeResolver;
import manifold.api.json.schema.JsonFormatType;
import manifold.api.util.ManStringUtil;

/**
 * For use with a GraphQL server to automatically transform JSON Manifold format resolvers to GraphQL Scalars e.g.,:
 * <p/>{@code GqlScalars.transformFormatTypeResolvers().forEach(runtimeWiringBuilder::scalar);}
 */
@SuppressWarnings("unused")
public class GqlScalars
{
  public static Collection<GraphQLScalarType> transformFormatTypeResolvers()
  {
    Set<GraphQLScalarType> scalars = new HashSet<>();
    for( IJsonFormatTypeResolver resolver: FormatTypeResolvers.get() )
    {
      resolver.getFormats().forEach( format -> {
        JsonFormatType jsonFormatType = resolver.resolveType( format );
        scalars.add( GraphQLScalarType.newScalar()
          .name( ManStringUtil.toPascalCase( format ) )
          .description( "Support values of type: " + jsonFormatType.getJavaType().getTypeName() )
          .coercing( makeCoercer( jsonFormatType, resolver ) )
          .build() );
      } );
    }
    return scalars;
  }

  private static Coercing makeCoercer( JsonFormatType jsonFormatType, IJsonFormatTypeResolver resolver )
  {
    return new Coercing()
    {
      @Override
      public Object parseValue( Object input ) throws CoercingParseValueException
      {
        try
        {
          return resolver.coerce( input, jsonFormatType.getJavaType() );
        }
        catch( Exception e )
        {
          throw new CoercingParseValueException( e );
        }
      }

      @Override
      public Object serialize( Object dataFetcherResult ) throws CoercingSerializeException
      {
        // The Gql interfaces know how to convert value
        return dataFetcherResult;
      }

      @Override
      public Object parseLiteral( Object input ) throws CoercingParseLiteralException
      {
        if( input instanceof StringValue && !((StringValue)input).isEqualTo( null ) )
        {
          return parseValue( ((StringValue)input).getValue() );
        }
        else
        {
          throw new CoercingParseLiteralException( "Empty 'StringValue' provided." );
        }
      }
    };
  }
}
