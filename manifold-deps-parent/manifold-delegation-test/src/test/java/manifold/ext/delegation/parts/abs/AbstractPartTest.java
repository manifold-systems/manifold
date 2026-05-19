package manifold.ext.delegation.parts.abs;

import junit.framework.TestCase;

public class AbstractPartTest extends TestCase
{
  public void testAbstractFooPart()
  {
    MyRoot myRoot = new MyRoot();
    assertEquals( "foo : AbstractFooPart.foo", myRoot.foo( "foo" ) );
    assertEquals( "bar : MyRoot.bar", myRoot.bar( "bar" ) );
  }

  public void testAbstractFooPart_Gen()
  {
    MyGenericRoot<String> myRoot = new MyGenericRoot<>();
    assertEquals( "foo : AbstractFooPart.foo", myRoot.foo( "foo" ) );
    assertEquals( "bar : MyRoot.bar", myRoot.bar( "bar" ) );
  }
}
