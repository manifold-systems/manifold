package manifold.api.host;

import abc.foobar;
import junit.framework.TestCase;

/**
 */
public class FoobarTest extends TestCase
{
  public void testFoobar()
  {
    foobar fb = foobar.create();
    fb.setFoo( foobar.foo.create() );
    fb.getFoo().setBar( foobar.bar.create() );
  }

}
