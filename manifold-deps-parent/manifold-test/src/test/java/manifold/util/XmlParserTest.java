package manifold.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;
import manifold.api.util.xml.XmlAttribute;
import manifold.api.util.xml.XmlElement;
import manifold.api.util.xml.XmlNamedPart;
import manifold.api.util.xml.XmlParser;
import manifold.api.util.xml.XmlTerminal;

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
