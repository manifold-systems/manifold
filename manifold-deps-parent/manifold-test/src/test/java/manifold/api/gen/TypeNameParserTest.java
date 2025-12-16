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

  public void testWithAnno()
  {
    assertType( "@Nullable List", "List" );
    assertType( "java.util. @Nullable List", "java.util.List" );
    assertType( "java.util.@Nullable List", "java.util.List" );
    assertType( "java.util.@Nullable List[]", "java.util.List[]" );
    assertType( "Function<String, @Nullable K>", "Function<String, K>" );
    assertType( "Function<String, @Nullable(\"hi\", 888, null, -6.7) K>", "Function<String, K>" );
    assertType( "Function<String, @org.jetbrains.annotations.Nullable K>", "Function<String, K>" );
    assertType( "java.util. @Nullable(\"hi\", 888, null, -6.7) List", "java.util.List" );
    assertType( "java.util. @Nullable(\"hi\", @Nullable(\"hi\", 888, null, -6.7), 888, null, -6.7) List", "java.util.List" );
    assertType( "String @A [] @B []", "String[][]" );
    assertType( "Map<@Nullable ? extends @Nullable Number, ? super String>", "Map<? extends Number, ? super String>" );
  }

  private void assertType( String fqn )
  {
    assertType( fqn, fqn );
  }
  private void assertType( String fqn, String fqnNoAnnos )
  {
    TypeNameParser parser = new TypeNameParser( fqn );
    TypeNameParser.Type type = parser.parse();
    assertEquals( fqnNoAnnos, type.getFullName() );

    SrcType srcType = new SrcType( fqnNoAnnos );
    StringBuilder sb = new StringBuilder();
    srcType.render( sb, 0 );
    assertEquals( fqnNoAnnos, sb.toString() );
  }
}
