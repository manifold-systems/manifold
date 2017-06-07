package manifold.api.gen;

import java.util.List;
import junit.framework.TestCase;

/**
 */
public class TypeNameParserTest extends TestCase
{
  public void testParser()
  {
    assertType( "int" );
    assertType( "int[]" );
    assertType( "int[][]" );
    assertType( String.class.getName() );
    assertType( String.class.getName() + "[]" );
    assertType( List.class.getName() + "<" + String.class.getName() + ">" );
    assertType( List.class.getName() + "<" + String.class.getName() + ">[]" );
    assertType( "java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<java.lang.String>>>" );
    assertType( "java.util.Map<java.lang.String, java.util.Map<java.util.List<java.lang.String>, java.util.List<java.lang.String>>>" );
    assertType( "java.util.Map<? extends java.lang.String, java.util.Map<java.util.List<java.lang.String>, java.util.List<java.lang.String>>>" );
    assertType( "java.util.Map<? extends java.lang.String, java.util.Map<? extends java.util.List<java.lang.String>, java.util.List<java.lang.String>>>" );
    assertType( "java.util.Map<? super java.lang.String, java.util.Map<? super java.util.List<java.lang.String>, java.util.List<java.lang.String>>>" );
    assertType( "E extends java.util.List<java.lang.String>" );
    assertType( "E super java.util.ArrayList<java.lang.String>" );
  }

  private void assertType( String fqn )
  {
    TypeNameParser parser = new TypeNameParser( fqn );
    TypeNameParser.Type type = parser.parse();
    assertEquals( fqn, type.getFullName() );

    SrcType srcType = new SrcType( fqn );
    StringBuilder sb = new StringBuilder();
    srcType.render( sb, 0 );
    assertEquals( fqn, sb.toString() );
  }
}
