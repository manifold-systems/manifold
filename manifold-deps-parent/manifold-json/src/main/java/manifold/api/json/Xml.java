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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.Bindings;
import manifold.api.util.Pair;
import manifold.api.util.xml.XmlAttribute;
import manifold.api.util.xml.XmlElement;
import manifold.api.util.xml.XmlParser;
import manifold.api.util.xml.XmlTerminal;
import manifold.ext.DataBindings;

public class Xml
{
  public static final String XML_DEFAULT_ROOT = "root_object";
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
      DataBindings bindings = transform( new DataBindings(), elem, withTokens );
      if( bindings.size() == 1 )
      {
        Object root = bindings.get( XML_DEFAULT_ROOT );
        if( root instanceof DataBindings )
        {
          bindings = (DataBindings)bindings.get( XML_DEFAULT_ROOT );
        }
      }
      return bindings;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private static DataBindings transform( DataBindings parent, XmlElement elem, boolean withTokens )
  {
    // Add to parent
    DataBindings children = new DataBindings();
    parent.put( elem.getName().getRawText(), withTokens ? makeTokensValue( elem, children ) : children );

    // Attributes
    for( Map.Entry<String, XmlAttribute> entry: elem.getAttributes().entrySet() )
    {
      children.put( entry.getKey(), withTokens ? makeTokensValue( entry.getValue() ) : entry.getValue().getValue() );
    }
    
    // Element text
    if( elem.getRawContent() != null )
    {
      children.put( XML_ELEM_CONTENT, elem.getRawContent().getRawText().trim() );
    }

    // Child Elements
    Map<String, List<DataBindings>> map = new LinkedHashMap<>();
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
        List<Object> listValues = list.stream().map( e -> e.get( entry.getKey() ) ).collect( Collectors.toList() );
        children.put( entry.getKey(), withTokens ? makeTokensValue( listValues )  : listValues );
      }
    }
    return parent;
  }


  private static Object makeTokensValue( XmlElement elem, Bindings value )
  {
    Token keyToken = makeToken( elem.getName() );
    return new Pair<>( new Token[]{keyToken, null}, value );
  }

  private static Object makeTokensValue( List<Object> value )
  {
    Object item = value.get( 0 );
    if( item instanceof Pair )
    {
      // Use the location of the first occurrence
      return new Pair<>( ((Pair)item).getFirst(), value );
    }
    return value;
  }

  private static Object makeTokensValue( XmlAttribute attr )
  {
    Token keyToken = makeToken( attr.getName() );
    XmlTerminal rawValue = attr.getRawValue();
    Token valueToken = rawValue == null ? null : makeToken( rawValue );
    return new Pair<>( new Token[]{keyToken, valueToken}, attr.getValue() );
  }

  private static Token makeToken( XmlTerminal value )
  {
    return new Token( TokenType.STRING, value.getRawText(), value.getOffset(), value.getLine(), -1 );
  }

}
