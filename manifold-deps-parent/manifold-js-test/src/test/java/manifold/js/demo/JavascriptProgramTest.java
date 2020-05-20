package manifold.js.demo;

import demo.JavascriptProgram;

import org.junit.Test;

import static org.junit.Assert.*;

public class JavascriptProgramTest {

  @Test
  public void testBasicJavascript() {
    assertEquals("Hello from Javascript", JavascriptProgram.exampleFunction("Hello"));

    assertEquals(1.0, asDouble( JavascriptProgram.nextNumber() ), 0 );
    assertEquals(2.0, asDouble( JavascriptProgram.nextNumber() ), 0 );
    assertEquals(3.0, asDouble( JavascriptProgram.nextNumber() ), 0 );
  }

  @Test
  public void testFileFragment()
  {
//    int value = (int)"[>.js<] 3 + 4 + 5";
//    assertEquals( 12, value );
  }

  /**
   * needed for testing with Java 8 and Java 9 (nashorn changed behavior between those versions)
   */
  private double asDouble( Object n )
  {
    if( n instanceof Number )
    {
      return ((Number)n).doubleValue();
    }
    throw new RuntimeException( "Expecting Number, but found: " + n.getClass().getName() );
  }
}
