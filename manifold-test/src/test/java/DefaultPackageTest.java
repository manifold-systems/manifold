import junit.framework.TestCase;

/**
 */
public class DefaultPackageTest extends TestCase
{
  public void testDefaultPackage()
  {
    assertEquals( "value1", DefaultPackageProps.Value1.toString() );
    assertEquals( "sub", DefaultPackageProps.Value1.Sub );
  }
}
