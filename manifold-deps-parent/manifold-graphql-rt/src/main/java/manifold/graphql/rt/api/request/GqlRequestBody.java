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

import java.util.*;

import manifold.graphql.rt.api.Config;
import manifold.rt.api.Bindings;
import manifold.json.rt.api.IJsonBindingsBacked;
import manifold.json.rt.api.DataBindings;
import manifold.ext.rt.api.IProxyFactory;
import manifold.ext.rt.api.Structural;

/**
 * Wraps the GraphQL request body consisting of both the query and variables.
 * @param <V> the query type as defined in the GraphQL query schema
 */
@Structural(factoryClass = GqlRequestBody.ProxyFactory.class)
public interface GqlRequestBody<V> extends IJsonBindingsBacked
{
  static <V> GqlRequestBody<V> create( String query, V variables )
  {
    DataBindings bindings = new DataBindings();
    bindings.put( "query", query );
    bindings.put( "variables", maybeRemoveNulls( (Bindings)variables ) );

    //noinspection unchecked
    return (GqlRequestBody<V>)bindings;
  }

  /**
   * Recursively remove entries with `null` values since the absence of a field and a field
   * with a `null` value are treated equally. The reason for removing them is mostly due to
   * tooling that does not handle null well (Nasdaq), however it may also help with large,
   * sparsely populated payloads.
   */
  static Bindings maybeRemoveNulls( Bindings variables )
  {
    if( !Config.instance().isRemoveNullConstraintValues() )
    {
      return variables;
    }

    return _maybeRemoveNulls( variables, new HashSet<>() );
  }
  static Bindings _maybeRemoveNulls( Bindings variables, Set<Integer> visited )
  {
    int identity = System.identityHashCode( variables );
    if( visited.contains( identity ) )
    {
      return variables;
    }
    visited.add( identity );

    for( Iterator<Map.Entry<String, Object>> iter = variables.entrySet().iterator(); iter.hasNext(); )
    {
      Map.Entry<String, Object> entry = iter.next();
      Object value = entry.getValue();
      if( value instanceof Bindings )
      {
        _maybeRemoveNulls( (Bindings)value, visited );
      }
      else if( value instanceof List )
      {
        removeNulls( (List)value, visited );
      }
      else if( value == null )
      {
        iter.remove();
      }
    }
    return variables;
  }

  static void removeNulls( List list, Set<Integer> visited )
  {
    int identity = System.identityHashCode( list );
    if( visited.contains( identity ) )
    {
      return;
    }
    visited.add( identity );

    for( Object item: list )
    {
      if( item instanceof List )
      {
        removeNulls( (List)item, visited );
      }
      else if( item instanceof Bindings )
      {
        _maybeRemoveNulls( (Bindings)item, visited );
      }
      // not removing direct null items
    }
  }

  /**
   * The static query defined in the GraphQL query schema.
   */
  @SuppressWarnings("unused") // used for tests
  default String getQuery()
  {
    return (String)getBindings().get( "query" );
  }

  /**
   * The query variable constraints.
   */
  @SuppressWarnings("unused")
  default V getVariables()
  {
    //noinspection unchecked

    return (V)getBindings().get( "variables" );
  }

  class ProxyFactory implements IProxyFactory<Map, GqlRequestBody>
  {
    @Override
    public GqlRequestBody proxy( Map map, Class<GqlRequestBody> iface )
    {
      //noinspection unchecked
      DataBindings bindings = map instanceof Bindings ? (DataBindings)map : new DataBindings( map );

      // DO NOT CHANGE THIS TO A LAMBDA, YOU WILL HAVE BAD LUCK FOR 9 YEARS
      //noinspection Convert2Lambda
      return new GqlRequestBody() {public DataBindings getBindings() {return bindings;}};
    }
  }
}
