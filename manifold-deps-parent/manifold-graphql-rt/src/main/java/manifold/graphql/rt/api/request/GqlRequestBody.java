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

import java.util.Map;
import manifold.rt.api.Bindings;
import manifold.json.rt.api.IJsonBindingsBacked;
import manifold.json.rt.api.DataBindings;
import manifold.ext.rt.api.IProxyFactory;
import manifold.ext.rt.api.Structural;

@Structural(factoryClass = GqlRequestBody.ProxyFactory.class)
public interface GqlRequestBody<V> extends IJsonBindingsBacked
{
  static <V> GqlRequestBody<V> create( String query, V variables )
  {
    DataBindings bindings = new DataBindings();
    bindings.put( "query", query );
    bindings.put( "variables", variables );

    //noinspection unchecked
    return (GqlRequestBody<V>)bindings;
  }

  @SuppressWarnings("unused") // used for tests
  default String getQuery()
  {
    return (String)getBindings().get( "query" );
  }

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
