package manifold.js.programs;

import programs.program_1;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BasicProgramTest {

  @Test
  public void testBasicStringReturn() {
    assertEquals("foo", program_1.returnsString());
  }

  @Test
  public void testBasicNumberReturn() {
    assertEquals(10, program_1.returnsNumber());
  }

  @Test
  public void testBasicObjectReturn() {
    Map o = (Map) program_1.returnsObject();
    assertEquals("bar", o.get("foo"));
  }

  @Test
  public void testBasicTypedReturn() {
    String str = program_1.returnsStringAsString();
    assertEquals("foo", str);
  }

}
