package manifold.ext.props;

import manifold.ext.props.rt.api.var;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OuterPropertyTest
{
  @Test
  public void testOuterProperty()
  {
    assertEquals( "hi", new Outer().testMe() );
  }

  static class Outer
  {
    @var String string;

    String testMe()
    {
      return new Inner().useOuterProperty();
    }

    class Inner
    {
      String useOuterProperty()
      {
        string = "hi";
        return string;
      }
    }
  }
}