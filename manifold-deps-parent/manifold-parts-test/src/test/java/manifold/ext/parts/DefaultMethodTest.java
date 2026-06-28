package manifold.ext.parts;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class DefaultMethodTest extends TestCase
{
  public void testDefaultMethod()
  {
    RootA rootA = new RootA();
    assertEquals( "B.g", rootA.f() );

    RootA_WithDefaultImpl rootAWithDefault = new RootA_WithDefaultImpl();
    assertEquals( "RootA_WithDefaultImpl.g", rootAWithDefault.f() );

    RootB rootB = new RootB();
    assertEquals( "B.g", rootB.f() );

    RootB_WithDefaultImpl rootBWithDefault = new RootB_WithDefaultImpl();
    assertEquals( "RootB_WithDefaultImpl.g", rootBWithDefault.f() );
  }

  interface A {
    default String f() { return g(); }
    String g();
  }

  interface B extends A {
    default String g() { return "B.g"; }
  }

  static @part class PartB implements B
  {
  }

  static @part class PartB_WithDefaultOverride implements B
  {
    @Override
    public String g()
    {
      return "PartB_WithDefaultOverride.g";
    }
  }

  class RootA implements A
  {
    @link A a = new PartB();
  }
  
  class RootA_WithDefaultImpl implements A
  {
    @link A a = new PartB();

    @Override
    public String g()
    {
      return "RootA_WithDefaultImpl.g";
    }
  }

  class RootB implements B
  {
    @link B b = new PartB();
  }

  class RootB_WithDefaultImpl implements B
  {
    @link B b = new PartB();

    @Override
    public String g()
    {
      return "RootB_WithDefaultImpl.g";
    }
  }

}

