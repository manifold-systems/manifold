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

import manifold.ext.api.IBindingsBacked;
import manifold.json.extensions.java.net.URL.ManUrlExt;
import manifold.json.extensions.javax.script.Bindings.ManBindingsExt;

/**
 * A base interface for all JSON and YAML types with methods to transform bindings to/from JSON and YAML
 * and to conveniently use the Bindings for JSON and YAML Web services.
 */
public interface IJsonBindingsBacked extends IBindingsBacked
{
  /**
   * Serializes this instance to a JSON formatted String
   *
   * @return This instance serialized to a JSON formatted String
   */
  default String toJson()
  {
    return ManBindingsExt.toJson( getBindings() );
  }

  /**
   * Serializes this instance to a YAML formatted String
   *
   * @return This instance serialized to a YAML formatted String
   */
  default String toYaml()
  {
    return ManBindingsExt.toJson( getBindings() );
  }

  /**
   * Serializes this instance to an XML formatted String
   *
   * @return This instance serialized to an XML formatted String
   */
  default String toXml()
  {
    return ManBindingsExt.toXml( getBindings() );
  }

  /**
   * Serializes this instance to an XML formatted String
   *
   * @param name the root name for the XML
   *
   * @return This instance serialized to an XML formatted String
   */
  default String toXml( String name )
  {
    return ManBindingsExt.toXml( getBindings(), name );
  }

  /**
   * Provide methods to load an instance of this interface from JSON and YAML sources such
   * as String, File, and URL (get/post).
   *
   * @param <E> The sub-interface extending {@link IJsonBindingsBacked}
   */
  class Loader<E>
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

    public E fromJsonUrl( java.net.URL url, javax.script.Bindings json )
    {
      return (E)ManUrlExt.postForJsonContent( url, json );
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

    public E fromYamlUrl( java.net.URL url, javax.script.Bindings yaml )
    {
      return (E)ManUrlExt.postForYamlContent( url, yaml );
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
}
