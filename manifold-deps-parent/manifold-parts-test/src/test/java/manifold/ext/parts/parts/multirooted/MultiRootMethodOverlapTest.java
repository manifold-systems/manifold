package manifold.ext.parts.parts.multirooted;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class MultiRootMethodOverlapTest extends TestCase
{
  public void testOverlap()
  {
    String result = new MyRoot().a();
    assertEquals( "AbPart.a : MyRoot.foo : MyB.foo", result );
  }

  interface A {
    String a();
    String foo(String s);
  }
  interface B {
    String foo(String s);
  }

  static @part class BPart implements B
  {
    @Override
    public String foo(String s)
    {
      return "MyB.foo";
    }
  }

  static @part class AbPart implements B, A
  {
    @link B b = new BPart();

    @Override
    public String a()
    {
      return "AbPart.a : " + ((A)this).foo("hi"); // cast is required here, otherwise compile error: MSG_AMBIGUOUS_RECEIVER
    }

    @Override
    public String foo(String s)
    {
      return b.foo(s);
    }
  }

  static class MyRoot implements A
  {
    @link A a = new AbPart();

    public String foo(String s)
    {
      return "MyRoot.foo : " + a.foo(s);
    }
  }
}
