package manifold.ext.parts.parts;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class WidestTest extends TestCase
{
  public void testWidest()
  {
    A root = new Root();
    assertEquals( "Root.a", root.aa() );
    assertEquals( "Root.a", root.aaa() );
  }

  interface A
  {
    String a();
    default String aa()
    {
      return a();
    }
    String aaa();
  }

  interface B
  {
    String b();
  }

  interface C extends A, B
  {
    String c();
  }

  static @part class PartA implements A
  {
    public String a()
    {
      return "PartA.a";
    }

    @Override
    public String aaa()
    {
      return a();
    }
  }

  static @part class PartB implements B
  {
    public String b()
    {
      return "PartB.b";
    }
  }

  static @part class PartC implements C
  {
    @link A a = new PartA();
    @link B b = new PartB();

    public String c()
    {
      return a.a() + b.b();
    }
  }

  static @part class PartD implements C
  {
    @link C c = new PartC();
  }

  static class Root implements A
  {
    @link A a = new PartD();

    public String a()
    {
      return "Root.a";
    }
  }
}
