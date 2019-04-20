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

@Structural(factoryClass = IGqlRequestArguments.ProxyFactory.class)
public interface IGqlRequestArguments<V> extends IJsonBindingsBacked
{
  static <V> IGqlRequestArguments<V> create( String query, V variables )
  {
    DataBindings bindings = new DataBindings();
    bindings.put( "query", query );
    bindings.put( "variables", variables );

    //noinspection unchecked
    return (IGqlRequestArguments<V>)bindings;
  }

  default String getQuery()
  {
    return (String)getBindings().get( "query" );
  }

  default V getVariables()
  {
    //noinspection unchecked

    return (V)getBindings().get( "variables" );
  }

  class Proxy implements IGqlRequestArguments
  {
    private final Bindings _bindings;

    private Proxy( Bindings bindings )
    {
      _bindings = bindings;
    }

    @Override
    public Bindings getBindings()
    {
      return _bindings;
    }
  }

  class ProxyFactory implements IProxyFactory<Map, IGqlRequestArguments>
  {
    @Override
    public IGqlRequestArguments proxy( Map bindings, Class<IGqlRequestArguments> iface )
    {
      if( !(bindings instanceof Bindings) )
      {
        //noinspection unchecked
        bindings = new DataBindings( bindings );
      }
      return new Proxy( (Bindings)bindings );
    }
  }
}
