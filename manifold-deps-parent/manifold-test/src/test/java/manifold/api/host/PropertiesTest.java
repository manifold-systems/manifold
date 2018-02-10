package manifold.api.host;

import junit.framework.TestCase;

import abc.MyProperties;
import gw.lang.SystemProperties;

/**
 */
public class PropertiesTest extends TestCase
{
  public void testProperties()
  {
    assertEquals( "Hello", MyProperties.MyProperty.toString() );
    assertEquals( "Sub Property", MyProperties.MyProperty.Sub );
    assertNotNull( SystemProperties.java.version );
  }
}
