package manifold.api.host;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import manifold.api.templ.DisableStringLiteralTemplates;


import static org.junit.Assert.assertNotEquals;

public class StringTemplateTest extends TestCase
{
  Integer _num = 10;

  public void testSimple()
  {
    int integer = 5;

    String value = "It is $integer o'clock";
    assertEquals( "It is 5 o'clock", value );

    value = "$integer$integer";
    assertEquals( "55", value );

    value = "$integer${integer}";
    assertEquals( "55", value );

    value = "$integer.$integer";
    assertEquals( "5.5", value );

    value = "$integer.${integer}";
    assertEquals( "5.5", value );

    value = "$integer";
    assertEquals( "5", value );

    value = " $integer";
    assertEquals( " 5", value );

    value = " $integer ";
    assertEquals( " 5 ", value );

    value = "$integer ";
    assertEquals( "5 ", value );

    value = "\n$integer";
    assertEquals( "\n5", value );

    value = "\n$integer\n";
    assertEquals( "\n5\n", value );

    value = "$integer\n";
    assertEquals( "5\n", value );
  }

  public void testExprEasy()
  {
    int integer = 5;

    String value = "It is ${integer} o'clock";
    assertEquals( "It is 5 o'clock", value );

    value = "${integer}${integer}";
    assertEquals( "55", value );

    value = "${integer}$integer";
    assertEquals( "55", value );

    value = "${integer}.${integer}";
    assertEquals( "5.5", value );

    value = "$integer.${integer}";
    assertEquals( "5.5", value );

    value = "${integer}";
    assertEquals( "5", value );

    value = " ${integer}";
    assertEquals( " 5", value );

    value = " ${integer} ";
    assertEquals( " 5 ", value );

    value = "${integer} ";
    assertEquals( "5 ", value );

    value = "\n${integer}";
    assertEquals( "\n5", value );

    value = "\n${integer}\n";
    assertEquals( "\n5\n", value );

    value = "${integer}\n";
    assertEquals( "5\n", value );

    value = "$_num";
    assertEquals( "10", value );

    value = "$this";
    assertEquals( "whatever", value );
  }

  public void testExprMore()
  {
    String value = "${\"hi\"}";
    assertEquals( "hi", value );

    value = "${5 + 3}";
    assertEquals( "8", value );

    value = "${_num}";
    assertEquals( "10", value );

    value = "${this._num}";
    assertEquals( "10", value );

    List<Class> classes = Arrays.asList( String.class, Integer.class );

    value = "Count: ${classes.size()}";
    assertEquals( "Count: 2", value );

    value = "Name: ${classes.get(1).getSimpleName()}";
    assertEquals( "Name: Integer", value );

    value = "${classes.size()}${classes.size()}";
    assertEquals( "22", value );

    value = "${classes.size() + classes.size() + 2}";
    assertEquals( "6", value );
  }

  @DisableStringLiteralTemplates
  public void testDisableStringLiteralTemplates_Method()
  {
    String value = "${\"hi\"}";
    assertNotEquals( "hi", value );

    @DisableStringLiteralTemplates(false)
    String value2 = "${\"hi\"}";
    assertEquals( "hi", value2 );
  }

  public void testDisableStringLiteralTemplates_Statement()
  {
    String value = "${\"hi\"}";
    assertEquals( "hi", value );

    @DisableStringLiteralTemplates
    String value2 = "${\"hi\"}";
    assertNotEquals( "hi", value2 );

    String value3 = "${\"hi\"}";
    assertEquals( "hi", value3 );
  }

  public void testInner()
  {
    assertEquals( "hi", new TestInner().testMe() );
    assertNotEquals( "hi", new TestInnerDisabled().testMe() );
    assertEquals( "hi", new TestInnerDisabled().testMeEnabled() );
    assertNotEquals( "hi", new TestInnerDisabled_Method().testMe() );
  }

  static class TestInner
  {
    String testMe()
    {
      return "${\"hi\"}";
    }
  }

  @DisableStringLiteralTemplates
  static class TestInnerDisabled
  {
    String testMe()
    {
      return "${\"hi\"}";
    }

    @DisableStringLiteralTemplates(false)
    String testMeEnabled()
    {
      return "${\"hi\"}";
    }
  }

  static class TestInnerDisabled_Method
  {
    @DisableStringLiteralTemplates
    String testMe()
    {
      return "${\"hi\"}";
    }
  }

  @DisableStringLiteralTemplates
  static class TestDisableInner
  {
    public void testDisableStringLiteralTemplates_Method()
    {
      String value = "${\"hi\"}";
      assertNotEquals( "hi", value );
    }
  }

  public String toString()
  {
    return "whatever";
  }
}
