package manifold.ext.parts.parts.defaultmethods;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class CrossInterfaceSelfCallTest extends TestCase
{
  public void testCrossInterfaceCall()
  {
    MyA myA = new MyA();
    assertEquals( "MyA.a", myA.aa() );
    assertEquals( "CPart.b MyA.a", myA.foo() );
    assertEquals( "CPart.c CPart.b MyA.a", myA.bar() );
  }

  interface A { String a(); default String aa() { return a(); } }
  interface B { String b(); }
  interface C extends A, B {
    default String a() {
      return b();
    }
  }

  @part
  class CPart implements C
  {
    public String b()
    {
      return "CPart.b " + ((A)this).a();
    }

    public String c()
    {
      return "CPart.c " + ((C)this).a();
    }

    // note, the generated default method for a() must dispatch through C, not A
    // because the default method is declared in C, not A. Only a composite implementing
    // C can field C's self-calls.
  }

  // MyA claims A so CPart claims C
  class MyA implements A
  {
    @link A a = new CPart();

    String foo() {
      return ((CPart)a).b();
    }
    String bar() {
      return ((CPart)a).c();
    }

    public String a()
    {
      return "MyA.a";
    }
  }
}
