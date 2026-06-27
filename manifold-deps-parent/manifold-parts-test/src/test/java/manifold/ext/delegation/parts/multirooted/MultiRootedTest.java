package manifold.ext.delegation.parts.multirooted;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class MultiRootedTest extends TestCase
{
  public void testMultiRooted()
  {
    String result = new Root().a();
    assertEquals( "Root.b MyBC.cc", result );
    // B is delegated from Root to MyAB, hence "Root.b"
    // C is *not* delegated from Root to MyAB, hence "MyBC.cc"
  }

  interface A {
    String a();
  }
  interface B {
    String b();
  }
  interface C {
    String c();
    String cc();
  }

  static @part class MyBC implements B, C {
    public String b() {return "MyBC.b";}
    public String c() {return b() + " " + cc();}
    public String cc() {return "MyBC.cc";}
  }
  static @part class MyAB implements A, B, C {
    @link MyBC myBC = new MyBC();

    public String a() {return c();}

    @Override
    public String b() {return "MyAB.b";}
  }
  static class Root implements A, B, C {
    @link({A.class, B.class}) MyAB myAB = new MyAB();

    @Override
    public String b() {return "Root.b";}

    @Override
    public String c()
    {
      return "Root.c";
    }

    @Override
    public String cc()
    {
      return "Root.cc";
    }
  }
}
