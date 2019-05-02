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

import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.IJsonBindingsBacked;
import manifold.ext.DataBindings;
import manifold.ext.api.IProxyFactory;
import manifold.ext.api.Structural;

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
      Bindings bindings = map instanceof Bindings ? (Bindings)map : new DataBindings( map );

      // DO NOT CHANGE THIS TO A LAMBDA, YOU WILL HAVE BAD LUCK FOR 9 YEARS
      //noinspection Convert2Lambda
      return new GqlRequestBody() {public Bindings getBindings() {return bindings;}};
    }
  }
}
