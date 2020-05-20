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

package manifold.xml.rt.parser;

import manifold.xml.rt.parser.antlr.XMLParser;

public class XmlRootElement extends XmlElement
{
  private final XmlElement _prolog;

  XmlRootElement( XMLParser.ElementContext ctx, XmlElement prolog )
  {
    super( ctx, null );
    _prolog = prolog;
  }

  public XmlElement getProlog()
  {
    return _prolog;
  }
}
