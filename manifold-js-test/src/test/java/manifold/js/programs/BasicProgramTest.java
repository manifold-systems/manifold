package manifold.js.programs;

import programs.program_1;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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

  @Test
  public void testIdentityArg() {
    assertEquals("foo", program_1.identity("foo"));
  }

  @Test
  public void testIdentityArgWithString() {
    assertEquals("foo", program_1.identityString("foo"));
  }

  @Test
  public void testTwoArgs() {
    assertEquals(3d, program_1.twoArgs(1, 2));
  }

  @Test
  public void testTypedReturn() {
    assertEquals("foo", program_1.returnAsString("foo"));
    try {
      program_1.returnAsString(1);
      fail("Should not reach here");
    } catch (ClassCastException e) {
      //success
    }
  }

  @Test
  public void testThreeArgs() {
    assertEquals(1, program_1.threeArgs(1, 2, true));
    assertEquals(2, program_1.threeArgs(1, 2, false));
  }

}
