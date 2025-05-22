package manifold.ext.params;


import junit.framework.TestCase;

public class BarTest extends TestCase
{
  // since most tests use inner classes, this one is just to test that separate files work too. Initially this test failed
  // due to annotating some generated methods with SYNTHETIC, which the compiler does not allow directly from external source
  // (they are generated with BRIDGE now, which is more suitable for their purpose anyway).
  public void testFoo()
  {
    String s = new Foo().optionalParamsNameAge(name:"hi", age:8);
  }
}
