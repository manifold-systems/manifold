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

package manifold.api.util.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import manifold.api.util.xml.gen.XMLParser;

public class XmlElement extends XmlNamedPart
{
  private static final String CDATA_START = "<![CDATA[";
  private static final String CDATA_END = "]]>";

  private List<XmlElement> _children;
  private Map<String, XmlAttribute> _attributes;
  private XmlTerminal _content;

  XmlElement( XMLParser.ElementContext ctx, XmlElement parent )
  {
    super( parent, ctx.start.getStartIndex(), ctx.stop.getStopIndex() - ctx.start.getStartIndex() + 1, ctx.start.getLine() );
    setName( new XmlTerminal( ctx.Name( 0 ).getSymbol(), this ) );
    _children = Collections.emptyList();
    _attributes = Collections.emptyMap();
  }

  public List<XmlElement> getChildren()
  {
    return _children;
  }
  void addChild( XmlElement child )
  {
    if( _children.isEmpty() )
    {
      _children = new ArrayList<>();
    }
    _children.add( child );
  }

  /**
   * @return attributes ordered by appearance in the parent element
   */
  public Map<String, XmlAttribute> getAttributes()
  {
    return _attributes;
  }
  void addAttribute( XmlAttribute attr )
  {
    if( _attributes.isEmpty() )
    {
      _attributes = new LinkedHashMap<>();
    }
    _attributes.put( attr.getName().getRawText(), attr );
  }

  public String getContent()
  {
    return _content == null ? null : getActualValue( _content.getRawText() );
  }

  private String getActualValue( String rawText )
  {
    if( rawText == null )
    {
      return null;
    }

    rawText = rawText.trim();
    rawText = removeCDATA( rawText );
    rawText = rawText.trim();
    return rawText;
  }

  private String removeCDATA( String rawText )
  {
    if( rawText.startsWith( CDATA_START ) )
    {
      rawText = rawText.substring( CDATA_START.length() );
      if( rawText.endsWith( CDATA_END ) )
      {
        rawText = rawText.substring( 0, rawText.length() - CDATA_END.length() );
      }
    }
    return rawText;
  }

  public XmlTerminal getRawContent()
  {
    return _content;
  }
  void setRawContent( XmlTerminal content )
  {
    _content = content;
    if( content.getParent() != this )
    {
      throw new IllegalStateException( "Parent mismatch" );
    }
  }
}
