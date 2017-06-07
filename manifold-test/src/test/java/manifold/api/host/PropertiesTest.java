package manifold.api.host;

import junit.framework.TestCase;

import abc.MyProperties;

/**
 */
public class PropertiesTest extends TestCase
{
  public void testProperties()
  {
    assertEquals( "Hello", MyProperties.MyProperty );
  }
}
