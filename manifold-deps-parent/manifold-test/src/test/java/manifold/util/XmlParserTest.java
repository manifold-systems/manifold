package manifold.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;
import manifold.util.xml.XmlAttribute;
import manifold.util.xml.XmlElement;
import manifold.util.xml.XmlNamedPart;
import manifold.util.xml.XmlParser;
import manifold.util.xml.XmlTerminal;

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

    assertEquals( "stuff", stuff.getName().getText() );
    assertEquals( 2, stuff.getAttributes().size() );
    assertEquals( "one", stuff.getAttributes().get( "one" ).getName().getText() );
    assertEquals( "'hello'", stuff.getAttributes().get( "one" ).getValue().getText() );
    assertEquals( "two", stuff.getAttributes().get( "two" ).getName().getText() );
    assertEquals( "\"bye\"", stuff.getAttributes().get( "two" ).getValue().getText() );
    assertEquals( "\n  my stuff\n  ", stuff.getContent().getText() );
    assertEquals( 2, stuff.getChildren().size() );

      XmlElement things = stuff.getChildren().get( 0 );
      assertEquals( "things", things.getName().getText() );
      assertEquals( 2, things.getAttributes().size() );
      assertEquals( "one", things.getAttributes().get( "one" ).getName().getText() );
      assertEquals( "\"hello\"", things.getAttributes().get( "one" ).getValue().getText() );
      assertEquals( "two", things.getAttributes().get( "two" ).getName().getText() );
      assertEquals( "\"bye\"", things.getAttributes().get( "two" ).getValue().getText() );
      assertEquals( "\n    my things\n  ", things.getContent().getText() );
      assertEquals( 0, things.getChildren().size() );

      XmlElement other = stuff.getChildren().get( 1 );
      assertEquals( "other", other.getName().getText() );
      assertEquals( 2, other.getAttributes().size() );
      assertEquals( "one:a", other.getAttributes().get( "one:a" ).getName().getText() );
      assertEquals( "\"a\"", other.getAttributes().get( "one:a" ).getValue().getText() );
      assertEquals( "two", other.getAttributes().get( "two" ).getName().getText() );
      assertEquals( "\"b\"", other.getAttributes().get( "two" ).getValue().getText() );
      assertEquals( "<![CDATA[\n" +
                    "      <message> other things </message>\n" +
                    "    ]]>", other.getContent().getText() );
      assertEquals( 0, other.getChildren().size() );
  }

  private void assertPartsMatchSourceLocations( String xml, XmlNamedPart node )
  {
    if( node instanceof XmlElement )
    {
      assertPartLocation( xml, node.getName() );
      assertPartLocation( xml, ((XmlElement)node).getContent() );

      ((XmlElement)node).getAttributes().forEach( (a,attr) -> assertPartsMatchSourceLocations( xml, attr ) );
      ((XmlElement)node).getChildren().forEach( node1 -> assertPartsMatchSourceLocations( xml, node1 ) );
    }
    else if( node instanceof XmlAttribute )
    {
      assertPartLocation( xml, node.getName() );
      assertPartLocation( xml, ((XmlAttribute)node).getValue() );
    }
  }

  private void assertPartLocation( String xml, XmlTerminal part )
  {
    String text = part.getText();
    int offset = part.getOffset();
    int length = part.getLength();
    assertEquals( xml.substring( offset, offset + length ), text );
  }
}
