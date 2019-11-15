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

package manifold.api.xml.parser;

import manifold.api.util.ManStringUtil;
import manifold.api.xml.parser.antlr.XMLParser;

public class XmlAttribute extends XmlNamedPart
{
  private XmlTerminal _value;

  XmlAttribute( XMLParser.AttributeContext ctx, XmlElement parent )
  {
    super( parent, ctx.start.getStartIndex(), ctx.stop.getStopIndex() - ctx.start.getStartIndex() + 1, ctx.start.getLine() );
    setName( new XmlTerminal( ctx.Name().getSymbol(), this ) );
  }

  public String getValue()
  {
    XmlTerminal rawValue = getRawValue();
    return rawValue == null ? null : ManStringUtil.unquote( rawValue.getRawText() );
  }

  public XmlTerminal getRawValue()
  {
    return _value;
  }
  void setRawValue( XmlTerminal value )
  {
    _value = value;
    if( value.getParent() != this )
    {
      throw new IllegalStateException( "Parent mismatch" );
    }
  }
}
