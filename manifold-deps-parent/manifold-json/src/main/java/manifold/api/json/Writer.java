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

import java.io.IOException;
import java.util.List;
import javax.script.Bindings;
import manifold.util.JsonUtil;

/**
 * This class is used as part of the JSON API. It defines methods to write this JSON object
 * in various forms of formatted text including JSON, YAML, and XML.
 */
public class Writer
{
  private final Object _value;

  public Writer( Bindings jsonBindings )
  {
    _value = jsonBindings;
  }
  public Writer( List<?> jsonList )
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
    return JsonUtil.toJson( _value );
  }
  public void toJson( Appendable target )
  {
    try
    {
      target.append( JsonUtil.toJson( _value ) );
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
    StringBuilder sb = new StringBuilder();
    Yaml.toYaml( _value, sb );
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
    return JsonUtil.toXml( _value );
  }
  public void toXml( Appendable target )
  {
    try
    {
      target.append( JsonUtil.toXml( _value ) );
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
    StringBuilder sb = new StringBuilder();
    JsonUtil.toXml( _value, name, sb, 0 );
    return sb.toString();
  }
}
