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

import manifold.json.rt.Json;

import java.io.IOException;
import manifold.rt.api.Bindings;

/**
 * This class is used as part of the JSON API. It defines methods to write this JSON object
 * in various forms of formatted text including JSON, YAML, CSV, and XML.
 */
public class Writer
{
  private final Object _value;

  public Writer( Bindings jsonBindings )
  {
    _value = jsonBindings;
  }
  public Writer( Iterable<?> jsonList )
  {
    _value = jsonList;
  }
  public Writer( Object jsonValue )
  {
    _value = jsonValue;
  }

  /**
   * Serializes this instance to a JSON formatted String
   *
   * @return This instance serialized to a JSON formatted String
   */
  public String toJson()
  {
    return Json.toJson( _value );
  }
  public void toJson( Appendable target )
  {
    try
    {
      target.append( Json.toJson( _value ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Serializes this instance to a YAML formatted String
   *
   * @return This instance serialized to a YAML formatted String
   */
  public String toYaml()
  {
    IJsonBindingsCodec yaml = IJsonBindingsCodec.get( "YAML" );
    StringBuilder sb = new StringBuilder();
    yaml.encode( _value, sb );
    return sb.toString();
  }
  public void toYaml( Appendable target )
  {
    try
    {
      target.append( toYaml() );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Serializes this instance to an XML formatted String
   *
   * @return This instance serialized to an XML formatted String
   */
  public String toXml()
  {
    IJsonBindingsCodec xml = IJsonBindingsCodec.get( "XML" );
    return xml.encode( _value );
  }
  public void toXml( Appendable target )
  {
    try
    {
      IJsonBindingsCodec xml = IJsonBindingsCodec.get( "XML" );
      target.append( xml.encode( _value ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Serializes this instance to an XML formatted String
   *
   * @param name the root name for the XML
   *
   * @return This instance serialized to an XML formatted String
   */
  public String toXml( String name )
  {
    IJsonBindingsCodec xml = IJsonBindingsCodec.get( "XML" );
    StringBuilder sb = new StringBuilder();
    xml.encode( _value, name, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this instance to an CSV formatted String
   *
   * @return This instance serialized to an CSV formatted String
   */
  public String toCsv()
  {
    IJsonBindingsCodec csv = IJsonBindingsCodec.get( "CSV" );
    return csv.encode( _value );
  }
  public void toCsv( Appendable target )
  {
    try
    {
      IJsonBindingsCodec csv = IJsonBindingsCodec.get( "CSV" );
      target.append( csv.encode( _value ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Serializes this instance to a CSV formatted String
   *
   * @param name the root name for the CSV
   *
   * @return This instance serialized to an CSV formatted String
   */
  public String toCsv( String name )
  {
    IJsonBindingsCodec csv = IJsonBindingsCodec.get( "CSV" );
    StringBuilder sb = new StringBuilder();
    csv.encode( _value, name, sb, 0 );
    return sb.toString();
  }
}
