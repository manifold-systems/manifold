package manifold.ext.parts.parts.defaultmethods;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

import static java.lang.System.out;

public class CrossInterfaceSelfCallTest extends TestCase
{
  public void testSubinterfaceOverridesWithDefaultMethod()
  {
    MyA_withC myA = new MyA_withC();
    assertEquals( "MyA.a", myA.a() );
    assertEquals( "MyA.a", myA.aa() ); // calls MyA's override bc aa() is defined exclusively in A and MyA claims A
    assertEquals( "CPart.b MyA.a", myA.foo() );
    assertEquals( "CPart.c CPart.b MyA.a", myA.bar() ); // CPart's self-call on a() calls a() on C's slot because C's a() default overrides A and also requires a C implementor
  }

  interface A
  {
    String a();
    default String aa() { return a(); }
  }
  interface B
  {
    String b();
  }
  interface C extends A, B
  {
    default String a() { return b(); }
  }

  @part class CPart implements C
  {
    public String b()
    {
      return "CPart.b " + ((A)this).a();
    }

    public String c()
    {
      // no cast required, since C is the narrowest of the two interfaces defining a()
      // (C's override wins because the `this` that the default method must run under must implement C e.g., for cross-interface self-calls)
      return "CPart.c " + a();
    }

    // note, the generated default method for a() must dispatch through C, not A
    // because the default method declared in C overrides A. Only a composite implementing
    // C can field C's self-calls.
  }

  // MyA claims A so CPart claims C
  class MyA_withC implements A
  {
    @link A a = new CPart();

    String foo()
    {
      return ((CPart)a).b();
    }
    String bar()
    {
      return ((CPart)a).c();
    }

    public String a()
    {
      return "MyA.a";
    }
  }

  public void testSubInterfaceSelfCallsSuperInterface()
  {
    MyA_subCallsSuper myA = new MyA_subCallsSuper();
    myA.a();
  }

  interface A2 { void a(); void aa(); }
  interface C2 extends A2 { default void a() { aa(); } }
  @part class CPart2 implements C2 {
    public void aa() { out.println("CPart.aa"); }
  }
  class MyA_subCallsSuper implements A2 {
    @link A2 a = new CPart2();
    public void aa() { out.println("MyA.aa"); }
  }
}
