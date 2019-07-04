package manifold.js.programs;

import programs.program_1;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mozilla.javascript.Undefined.SCRIPTABLE_UNDEFINED;

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
  public void testStateIsPreservedInPrograms() {
    assertEquals(10d, asDouble( program_1.incrementAndGet()), 0);
    assertEquals(11d, asDouble( program_1.incrementAndGet()), 0);
    assertEquals(12d, asDouble( program_1.incrementAndGet()), 0);
  }

  @Test
  public void testStateCanBeUpdatedInPrograms() {
    assertEquals("foo", program_1.getY());
    assertEquals(SCRIPTABLE_UNDEFINED, program_1.setY("bar"));
    assertEquals("bar", program_1.getY());
  }

  @Test
  public void testThreeArgs() {
    assertEquals(1, program_1.threeArgs(1, 2, true));
    assertEquals(2, program_1.threeArgs(1, 2, false));
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
