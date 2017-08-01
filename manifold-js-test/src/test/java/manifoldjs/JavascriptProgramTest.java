package manifoldjs;

import demo.JavascriptProgram;

import org.junit.Test;

import static org.junit.Assert.*;

public class JavascriptProgramTest {

  @Test
  public void testBasicJavascript() {
    assertEquals("Hello from Javascript", JavascriptProgram.exampleFunction("Hello"));

    assertEquals(1, JavascriptProgram.nextNumber());
    assertEquals(2, JavascriptProgram.nextNumber());
    assertEquals(3, JavascriptProgram.nextNumber());
  }

}
