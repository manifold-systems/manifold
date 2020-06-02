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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import manifold.rt.api.Bindings;

import manifold.json.rt.api.DataBindings;
import manifold.json.rt.parser.Token;
import manifold.json.rt.parser.TokenType;
import manifold.rt.api.util.Pair;
import manifold.xml.rt.parser.XmlAttribute;
import manifold.xml.rt.parser.XmlParser;
import manifold.xml.rt.parser.XmlTerminal;
import manifold.xml.rt.parser.XmlElement;

import static manifold.json.rt.Json.indent;
import static manifold.json.rt.Json.toBindings;

public class Xml
{
  public static final String XML_DEFAULT_ROOT = "root_object";
  public static final String XML_ELEM_CONTENT = "textContent";

  public static String toXml( Object jsonValue )
  {
    StringBuilder sb = new StringBuilder();

    jsonValue = toBindings( jsonValue );

    if( jsonValue instanceof Map )
    {
      toXml( jsonValue, null, sb, 0 );
    }
    else if( jsonValue instanceof Iterable )
    {
      toXml( jsonValue, "list", sb, 0 );
    }
    else
    {
      toXml( jsonValue, "item", sb, 0 );
    }
    return sb.toString();
  }

  public static void toXml( Object jsonValue, String name, StringBuilder target, int indent )
  {
    jsonValue = toBindings( jsonValue );

    if( jsonValue instanceof Map )
    {
      if( name == null )
      {
        Map map = (Map)jsonValue;
        if( map.size() == 1 )
        {
          // single entry with no name implies root, defer to the root
          Object rootKey = map.keySet().iterator().next();
          Object rootValue = map.get( rootKey );
          if( rootValue instanceof Pair )
          {
            rootValue = ((Pair)rootValue).getSecond();
          }
          toXml( rootValue, rootKey.toString(), target, indent );
          return;
        }
        else
        {
          //todo: factor out Xml.XML_DEFAULT_ROOT
          name = "root_object";
        }
      }
      toXml( (Map)jsonValue, name, target, indent );
    }
    else if( jsonValue instanceof Iterable )
    {
      toXml( (Iterable)jsonValue, name, target, indent );
    }
    else
    {
      toXml( String.valueOf( jsonValue ), name, target, indent );
    }
  }

  /**
   * Serializes this {@link Map} instance into an XML formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param name   The name of the root element to nest the Map XML
   * @param target A {@link StringBuilder} to write the XML in
   * @param indent The margin of spaces to indent the XML
   */
  private static void toXml( Map bindings, String name, StringBuilder target, int indent )
  {
    indent( target, indent );
    target.append( '<' ).append( name );
    if( bindings.size() > 0 )
    {
      for( Object key: bindings.keySet() )
      {
        Object value = bindings.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }

        value = toBindings( value );

        if( !(value instanceof Map) && !(value instanceof Iterable) && !key.equals( XML_ELEM_CONTENT ) )
        {
          target.append( " " ).append( key ).append( "=\"" ).append( value ).append( '"' );
        }
      }
      int count = 0;
      for( Object key: bindings.keySet() )
      {
        Object value = bindings.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }

        value = toBindings( value );

        if( value instanceof Map )
        {
          if( count == 0 )
          {
            target.append( ">\n" );
          }

          toXml( (Map)value, key.toString(), target, indent + 2 );

          count++;
        }
        else if( value instanceof Iterable )
        {
          if( count == 0 )
          {
            target.append( ">\n" );
          }

          toXml( (Iterable)value, key.toString(), target, indent + 2 );

          count++;
        }
        else if( value instanceof String && key.equals( XML_ELEM_CONTENT ) )
        {
          if( count == 0 )
          {
            target.append( ">\n" );
          }

          indent( target, indent + 2 );
          target.append( value ).append( "\n" );

          count++;
        }
      }

      if( count == 0 )
      {
        target.append( "/>\n" );
      }
      else
      {
        indent( target, indent );
        target.append( "</" ).append( name ).append( ">\n" );
      }
    }
    else
    {
      target.append( "/>\n" );
    }
  }

  private static void toXml( Iterable value, String name, StringBuilder target, int indent )
  {
    for( Object comp: value )
    {
      if( comp instanceof Pair )
      {
        comp = ((Pair)comp).getSecond();
      }

      comp = toBindings( comp );

      if( comp instanceof Map )
      {
        toXml( (Map)comp, name, target, indent );
      }
      else if( comp instanceof Iterable )
      {
        toXml( (Iterable)comp, name, target, indent );
      }
      else
      {
        toXml( String.valueOf( comp ), name, target, indent );
      }
    }
  }

  private static void toXml( String value, String name, StringBuilder target, int indent )
  {
    indent( target, indent );
    target.append( '<' ).append( name ).append( ">" );
    target.append( value );
    target.append( "</" ).append( name ).append( ">\n" );
  }

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
