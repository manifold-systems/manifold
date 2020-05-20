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
import java.util.stream.Collectors;
import manifold.rt.api.Bindings;
import manifold.json.rt.api.DataBindings;
import manifold.json.rt.api.IJsonBindingsBacked;
import manifold.ext.rt.api.IProxyFactory;
import manifold.ext.rt.api.Structural;

/**
 * Reflects the {@link graphql.GraphQLError} values encoded in the @{@code "errors"} response of a GraphQL request.
 */
@Structural(factoryClass = GqlError.ProxyFactory.class)
public interface GqlError extends IJsonBindingsBacked
{
  default String getMessage()
  {
    return (String)getBindings().get( "message" );
  }

  default String getType()
  {
    return String.valueOf( getBindings().get( "type" ) );
  }

  default String getClassification()
  {
    Map extensions = (Map)getBindings().get( "extensions" );
    return extensions == null ? null : String.valueOf( extensions.get( "classification" ) );
  }

  default List<String> getPath()
  {
    Object path = getBindings().get( "path" );
    if( path instanceof List )
    {
      return ((List<?>)path).stream().map( e -> String.valueOf( e ) ).collect( Collectors.toList() );
    }
    return null;
  }

  default List<Location> getLocations()
  {
    //noinspection unchecked
    return (List<Location>)getBindings().get( "locations" );
  }

  @Structural(factoryClass = GqlError.Location.ProxyFactory.class)
  interface Location extends IJsonBindingsBacked
  {
    default int getLine()
    {
      return (int)getBindings().get( "line" );
    }
    default int getColumn()
    {
      return (int)getBindings().get( "column" );
    }

    class ProxyFactory implements IProxyFactory<Map, Location>
    {
      @Override
      public Location proxy( Map map, Class<Location> iface )
      {
        //noinspection unchecked
        Bindings bindings = map instanceof Bindings ? (Bindings)map : new DataBindings( map );

        // DO NOT CHANGE THIS TO A LAMBDA, YOU WILL HAVE BAD LUCK FOR 9 YEARS
        //noinspection Convert2Lambda
        return new Location() {public Bindings getBindings() {return bindings;}};
      }
    }
  }

  class ProxyFactory implements IProxyFactory<Map, GqlError>
  {
    @Override
    public GqlError proxy( Map map, Class<GqlError> iface )
    {
      //noinspection unchecked
      Bindings bindings = map instanceof Bindings ? (Bindings)map : new DataBindings( map );

      // DO NOT CHANGE THIS TO A LAMBDA, YOU WILL HAVE BAD LUCK FOR 9 YEARS
      //noinspection Convert2Lambda
      return new GqlError() {public Bindings getBindings() {return bindings;}};
    }
  }
}
