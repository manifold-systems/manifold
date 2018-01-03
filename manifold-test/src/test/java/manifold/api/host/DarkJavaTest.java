package manifold.api.host;

import abc.Darkness;
import junit.framework.TestCase;

/**
 */
public class DarkJavaTest extends TestCase
{
  public void testDarkness()
  {
    Darkness darkness = new Darkness( "hi" );
    assertEquals( "hi", darkness.getName() );
    assertEquals( "bye", darkness.makeStuff( "bye" ).getStuff() );
  }
}
