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

import manifold.json.rt.api.IJsonBindingsTranslator;

public class XmlBindingsTranslator implements IJsonBindingsTranslator
{
  @Override
  public String getName()
  {
    return "XML";
  }

  @Override
  public String fromBindings( Object bindingsValue )
  {
    return Xml.toXml( bindingsValue );
  }

  @Override
  public void fromBindings( Object bindingsValue, StringBuilder target )
  {
    Xml.toXml( bindingsValue, "data", target, 0 );
  }

  @Override
  public void fromBindings( Object bindingsValue, String name, StringBuilder target, int indent )
  {
    Xml.toXml( bindingsValue, name, target, indent );
  }

  @Override
  public Object toBindings( String translation )
  {
    return Xml.fromXml( translation );
  }

  @Override
  public Object toBindings( String translation, boolean withTokens )
  {
    return Xml.fromXml( translation, withTokens );
  }

  @Override
  public Object toBindings( String translation, boolean withBigNumbers, boolean withTokens )
  {
    return Xml.fromXml( translation, withTokens );
  }
}
