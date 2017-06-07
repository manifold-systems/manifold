package manifold.api.host;

import abc.benis_png;
import javax.swing.ImageIcon;
import junit.framework.TestCase;

/**
 */
public class ImageTest extends TestCase
{
  public void testProperties()
  {
    assertTrue( benis_png.get() instanceof ImageIcon );
    assertEquals( 32, benis_png.get().getIconWidth() );
  }
}
