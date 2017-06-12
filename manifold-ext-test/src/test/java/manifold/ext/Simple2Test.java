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

    assertNotNull( abc.benis_png.get() );
    assertEquals( 32, abc.benis_png.get().width() );
    assertEquals( "benis32", abc.benis_png.get().myMethod( "benis" ) );
  }

}
