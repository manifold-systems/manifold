package manifold.ext;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 */
public class Simple2Test extends TestCase
{
  public void testMe()
  {
    List<String> l2 = new ArrayList<>();
    assertSame( l2, l2.find( l2 ) );
  }

}
