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

package manifold.json.rt.api;

import java.io.Reader;
import java.util.Arrays;

import manifold.json.rt.Json;
import manifold.json.rt.extensions.java.net.URL.ManUrlExt;
import manifold.rt.api.util.StreamUtil;

/**
 * This class is used as part of the JSON API. It provides methods to load an instance of a JSON interface from
 * potential JSON, XML, CSV, and YAML sources such as String, File, and URL (via HTTP GET).
 * <p/>
 * See {@link Requester}, obtained from a JSON API object's {@code send()} method, for methods to send an HTTP request
 * using GET, POST, PUT, PATCH, & DELETE.
 *
 * @param <E> The sub-interface extending {@link IJsonBindingsBacked} or {@link IJsonList}
 */
public class Loader<E>
{
  public E fromJson( String jsonText )
  {
    return (E)Json.fromJson( jsonText );
  }

  //
  // JSON methods
  //

  public E fromJsonUrl( String urlBase, String... urlSuffix )
  {
    try
    {
      StringBuilder url = new StringBuilder( urlBase );
      if( urlSuffix != null )
      {
        Arrays.stream( urlSuffix ).forEach( url::append );
      }

      return (E)ManUrlExt.getJsonContent( new java.net.URL( url.toString() ) );
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

  public E fromJsonReader( Reader reader )
  {
    try
    {
      return (E)Json.fromJson( StreamUtil.getContent( reader ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }


  //
  // YAML methods
  //

  public E fromYaml( String yamlText )
  {
    IJsonBindingsTranslator yaml = IJsonBindingsTranslator.get( "YAML" );
    return (E)yaml.toBindings( yamlText );
  }

  public E fromYamlUrl( String urlBase, String... urlSuffix )
  {
    try
    {
      StringBuilder url = new StringBuilder( urlBase );
      if( urlSuffix != null )
      {
        Arrays.stream( urlSuffix ).forEach( url::append );
      }

      return (E)ManUrlExt.getYamlContent( new java.net.URL( url.toString() ) );
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

  public E fromYamlReader( Reader reader )
  {
    try
    {
      IJsonBindingsTranslator yaml = IJsonBindingsTranslator.get( "YAML" );
      return (E)yaml.toBindings( StreamUtil.getContent( reader ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }


  //
  // XML methods
  //

  public E fromXml( String xmlText )
  {
    IJsonBindingsTranslator xml = IJsonBindingsTranslator.get( "XML" );
    return (E)xml.toBindings( xmlText );
  }

  public E fromXmlUrl( String urlBase, String... urlSuffix )
  {
    try
    {
      StringBuilder url = new StringBuilder( urlBase );
      if( urlSuffix != null )
      {
        Arrays.stream( urlSuffix ).forEach( url::append );
      }

      return (E)ManUrlExt.getXmlContent( new java.net.URL( url.toString() ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public E fromXmlUrl( java.net.URL url )
  {
    return (E)ManUrlExt.getXmlContent( url );
  }

  public E fromXmlFile( java.io.File file )
  {
    try
    {
      return fromXmlUrl( file.toURI().toURL() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public E fromXmlReader( Reader reader )
  {
    try
    {
      IJsonBindingsTranslator xml = IJsonBindingsTranslator.get( "XML" );
      return (E)xml.toBindings( StreamUtil.getContent( reader ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }


  //
  // CSV methods
  //

  public E fromCsv( String csvText )
  {
    IJsonBindingsTranslator csv = IJsonBindingsTranslator.get( "CSV" );
    return (E)csv.toBindings( csvText );
  }

  public E fromCsvUrl( String urlBase, String... urlSuffix )
  {
    try
    {
      StringBuilder url = new StringBuilder( urlBase );
      if( urlSuffix != null )
      {
        Arrays.stream( urlSuffix ).forEach( url::append );
      }

      return (E)ManUrlExt.getCsvContent( new java.net.URL( url.toString() ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public E fromCsvUrl( java.net.URL url )
  {
    return (E)ManUrlExt.getCsvContent( url );
  }

  public E fromCsvFile( java.io.File file )
  {
    try
    {
      return fromCsvUrl( file.toURI().toURL() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public E fromCsvReader( Reader reader )
  {
    try
    {
      IJsonBindingsTranslator csv = IJsonBindingsTranslator.get( "CSV" );
      return (E)csv.toBindings( StreamUtil.getContent( reader ) );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
