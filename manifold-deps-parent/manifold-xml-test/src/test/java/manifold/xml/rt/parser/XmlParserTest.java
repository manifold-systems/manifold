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

package manifold.xml.rt.parser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import junit.framework.TestCase;

public class XmlParserTest extends TestCase
{
  public void testAttributes()
  {
    String xml =
      "<stuff one='hello' two=\"bye\">\n" +
      "  my stuff\n" +
      "  <things one=\"hello\" two=\"bye\">\n" +
      "    my things\n" +
      "  </things>\n" +
      "  <other one:a=\"a\" two=\"b\">\n" +
      "    <![CDATA[\n" +
      "      <message> other things </message>\n" +
      "    ]]>\n" +
      "  </other>\n" +
      "</stuff>\n";
    XmlElement stuff = XmlParser.parse( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) );

    assertPartsMatchSourceLocations( xml, stuff );

    assertEquals( "stuff", stuff.getName().getRawText() );
    assertEquals( 2, stuff.getAttributes().size() );
    assertEquals( "one", stuff.getAttributes().get( "one" ).getName().getRawText() );
    assertEquals( "'hello'", stuff.getAttributes().get( "one" ).getRawValue().getRawText() );
    assertEquals( "hello", stuff.getAttributes().get( "one" ).getValue() );
    assertEquals( "two", stuff.getAttributes().get( "two" ).getName().getRawText() );
    assertEquals( "bye", stuff.getAttributes().get( "two" ).getValue() );
    assertEquals( "\n  my stuff\n  ", stuff.getRawContent().getRawText() );
    assertEquals( "my stuff", stuff.getContent() );
    assertEquals( 2, stuff.getChildren().size() );

      XmlElement things = stuff.getChildren().get( 0 );
      assertEquals( "things", things.getName().getRawText() );
      assertEquals( 2, things.getAttributes().size() );
      assertEquals( "one", things.getAttributes().get( "one" ).getName().getRawText() );
      assertEquals( "\"hello\"", things.getAttributes().get( "one" ).getRawValue().getRawText() );
      assertEquals( "hello", things.getAttributes().get( "one" ).getValue() );
      assertEquals( "two", things.getAttributes().get( "two" ).getName().getRawText() );
      assertEquals( "\"bye\"", things.getAttributes().get( "two" ).getRawValue().getRawText() );
      assertEquals( "bye", things.getAttributes().get( "two" ).getValue() );
      assertEquals( "\n    my things\n  ", things.getRawContent().getRawText() );
      assertEquals( "my things", things.getContent() );
      assertEquals( 0, things.getChildren().size() );

      XmlElement other = stuff.getChildren().get( 1 );
      assertEquals( "other", other.getName().getRawText() );
      assertEquals( 2, other.getAttributes().size() );
      assertEquals( "one:a", other.getAttributes().get( "one:a" ).getName().getRawText() );
      assertEquals( "\"a\"", other.getAttributes().get( "one:a" ).getRawValue().getRawText() );
      assertEquals( "a", other.getAttributes().get( "one:a" ).getValue() );
      assertEquals( "two", other.getAttributes().get( "two" ).getName().getRawText() );
      assertEquals( "\"b\"", other.getAttributes().get( "two" ).getRawValue().getRawText() );
      assertEquals( "b", other.getAttributes().get( "two" ).getValue() );
      assertEquals( "<![CDATA[\n" +
                    "      <message> other things </message>\n" +
                    "    ]]>", other.getRawContent().getRawText() );
      assertEquals( "<message> other things </message>", other.getContent() );
      assertEquals( 0, other.getChildren().size() );
  }

  public void testProlog()
  {
    final String xml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<nothing/>";
    XmlRootElement nothing = (XmlRootElement)XmlParser.parse( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) );
    final XmlElement prolog = nothing.getProlog();
    final Map<String, XmlAttribute> attributes = prolog.getAttributes();
    assertEquals( 2, attributes.size() );
    final XmlAttribute version = attributes.get( "version" );
    assertEquals( "1.0", version.getValue() );
    final XmlAttribute encoding = attributes.get( "encoding" );
    assertEquals( "UTF-8", encoding.getValue() );
    assertEquals( "nothing", nothing.getName().getRawText() );
  }

  private void assertPartsMatchSourceLocations( String xml, XmlNamedPart node )
  {
    if( node instanceof XmlElement )
    {
      assertPartLocation( xml, node.getName() );
      assertPartLocation( xml, ((XmlElement)node).getRawContent() );

      ((XmlElement)node).getAttributes().forEach( (a,attr) -> assertPartsMatchSourceLocations( xml, attr ) );
      ((XmlElement)node).getChildren().forEach( node1 -> assertPartsMatchSourceLocations( xml, node1 ) );
    }
    else if( node instanceof XmlAttribute )
    {
      assertPartLocation( xml, node.getName() );
      assertPartLocation( xml, ((XmlAttribute)node).getRawValue() );
    }
  }

  private void assertPartLocation( String xml, XmlTerminal part )
  {
    String text = part.getRawText();
    int offset = part.getOffset();
    int length = part.getLength();
    assertEquals( xml.substring( offset, offset + length ), text );
  }
}
