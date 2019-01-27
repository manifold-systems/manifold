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

package manifold.api.json;

import manifold.json.extensions.java.net.URL.ManUrlExt;

/**
 * This class is used as part of the JSON API. It provides methods to load an instance of a JSON interface from
 * JSON and YAML sources such as String, File, and URL (via HTTP GET).
 *
 * @param <E> The sub-interface extending {@link IJsonBindingsBacked}
 */
public class Loader<E>
{
  public E fromJson( String jsonText )
  {
    return (E)Json.fromJson( jsonText );
  }

  public E fromJsonUrl( String url )
  {
    try
    {
      return (E)ManUrlExt.getJsonContent( new java.net.URL( url ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public E fromJsonUrl( java.net.URL url )
  {
    return (E)ManUrlExt.getJsonContent( url );
  }

  public E fromJsonFile( java.io.File file )
  {
    try
    {
      return (E)fromJsonUrl( file.toURI().toURL() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }



  public E fromYaml( String yamlText )
  {
    return (E)Yaml.fromYaml( yamlText );
  }

  public E fromYamlUrl( String url )
  {
    try
    {
      return (E)ManUrlExt.getYamlContent( new java.net.URL( url ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public E fromYamlUrl( java.net.URL url )
  {
    return (E)ManUrlExt.getYamlContent( url );
  }

  public E fromYamlFile( java.io.File file )
  {
    try
    {
      return fromYamlUrl( file.toURI().toURL() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
