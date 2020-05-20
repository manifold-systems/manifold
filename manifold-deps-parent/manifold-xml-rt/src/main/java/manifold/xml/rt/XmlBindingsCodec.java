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

package manifold.xml.rt;

import manifold.json.rt.api.IJsonBindingsCodec;

public class XmlBindingsCodec implements IJsonBindingsCodec
{
  @Override
  public String getName()
  {
    return "XML";
  }

  @Override
  public String encode( Object jsonValue )
  {
    return Xml.toXml( jsonValue );
  }

  @Override
  public void encode( Object jsonValue, StringBuilder target )
  {
    Xml.toXml( jsonValue, "data", target, 0 );
  }

  @Override
  public void encode( Object jsonValue, String name, StringBuilder target, int indent )
  {
    Xml.toXml( jsonValue, name, target, indent );
  }

  @Override
  public Object decode( String encoded )
  {
    return Xml.fromXml( encoded );
  }

  @Override
  public Object decode( String encoded, boolean withTokens )
  {
    return Xml.fromXml( encoded, withTokens );
  }

  @Override
  public Object decode( String encoded, boolean withBigNumbers, boolean withTokens )
  {
    return Xml.fromXml( encoded, withTokens );
  }
}
