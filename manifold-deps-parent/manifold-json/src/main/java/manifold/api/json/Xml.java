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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.Bindings;
import manifold.api.util.Pair;
import manifold.api.util.xml.XmlAttribute;
import manifold.api.util.xml.XmlElement;
import manifold.api.util.xml.XmlParser;
import manifold.api.util.xml.XmlPart;
import manifold.api.util.xml.XmlTerminal;
import manifold.ext.DataBindings;

public class Xml
{
  public static final String XML_ELEM_CONTENT = "textContent";

  public static Bindings fromXml( String xml )
  {
    return fromXml( xml, false );
  }
  public static Bindings fromXml( String xml, boolean withTokens )
  {
    try( InputStream inputStream = new BufferedInputStream( new ByteArrayInputStream( xml.getBytes() ) ) )
    {
      XmlElement elem = XmlParser.parse( inputStream );
      return transform( new DataBindings(), elem, withTokens );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private static DataBindings transform( DataBindings parent, XmlElement elem, boolean withTokens )
  {
    DataBindings children = new DataBindings();
    parent.put( elem.getName().getRawText(), children );
    children.put( XML_ELEM_CONTENT,
      withTokens ? makeTokensValue( elem ) : elem.getRawContent() == null ? null : elem.getRawContent().getRawText().trim() );

    // Attributes
    for( Map.Entry<String, XmlAttribute> entry: elem.getAttributes().entrySet() )
    {
      children.put( entry.getKey(), withTokens ? makeTokensValue( entry.getValue() ) : entry.getValue().getValue() );
    }

    // Child Elements
    Map<String, List<DataBindings>> map = new HashMap<>();
    for( XmlElement child: elem.getChildren() )
    {
      map.computeIfAbsent( child.getName().getRawText(), k -> new ArrayList<>() )
        .add( transform( new DataBindings(), child, withTokens ) );
    }
    for( Map.Entry<String, List<DataBindings>> entry: map.entrySet() )
    {
      List<DataBindings> list = entry.getValue();
      if( list.size() == 1 )
      {
        children.putAll( list.get( 0 ) );
      }
      else
      {
        // Duplicates are put into a list and indirectly exposed through it
        children.put( entry.getKey(), list.stream().map( e -> e.get( entry.getKey() ) ).collect( Collectors.toList() ) );
      }
    }
    return parent;
  }


  private static Object makeTokensValue( XmlElement elem )
  {
    Token keyToken = makeToken( elem, elem.getName() );
    XmlTerminal rawContent = elem.getRawContent();
    Token valueToken = rawContent == null ? null : makeToken( rawContent, rawContent );
    return new Pair<>( new Token[]{keyToken, valueToken}, rawContent == null ? null : rawContent.getRawText().trim() );
  }

  private static Object makeTokensValue( XmlAttribute attr )
  {
    Token keyToken = makeToken( attr, attr.getName() );
    XmlTerminal rawValue = attr.getRawValue();
    Token valueToken = rawValue == null ? null : makeToken( rawValue, rawValue );
    return new Pair<>( new Token[]{keyToken, valueToken}, attr.getValue() );
  }

  private static Token makeToken( XmlPart xmlPart, XmlTerminal value )
  {
    return new Token( TokenType.STRING, value.getRawText(), xmlPart.getOffset(), xmlPart.getLine(), -1 );
  }

}
