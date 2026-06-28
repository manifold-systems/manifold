package manifold.ext.parts.parts.multirooted;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class SimpleMultiRootedTest extends TestCase
{
  public void testMultiRooted()
  {
    String result = new Root().a();
    assertEquals( "MyAB.b", result );
    // B is delegated from Root to MyAB, hence "Root.b"
  }

  interface A
  {
    String a();
  }

  interface B
  {
    String b();
  }

  static @part class MyAB implements A, B
  {
    public String a()
    {
      return b();
    }

    public String b()
    {
      return "MyAB.b";
    }
  }

  static class Root implements A, B
  {
    @link
    A a = new MyAB();

    public String b()
    {
      return "Root.b";
    }
  }
}
