package manifold.ext.parts.parts.multirooted;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class OverlapSelfCallTest extends TestCase
{
  public void testOverlapCall_TopologyB()
  {
    AB ab = new AB();
    A a = new AImpl( ab );
    assertEquals( "AImpl.z AB.z", a.a() );
  }

  public void testOverlapCallBoth_TopologyC()
  {
    AB ab = new AB();
    A a = new AImpl( ab );
    B b = new BImpl( ab );
    assertEquals( "BImpl.z AImpl.z", a.a() );
    assertEquals( "AImpl.z BImpl.z", b.b() );
  }
  public void testOverlapCallA_TopologyC()
  {
    A a = new AImpl( new AB() );
    assertEquals( "AB.z AImpl.z", a.a() );
  }
  public void testOverlapCallB_TopologyC()
  {
    B b = new BImpl( new AB() );
    assertEquals( "AB.z BImpl.z", b.b() );
  }

  interface Z { String z(); }
  interface A extends Z { String a(); }
  interface B extends Z { String b(); }
  static @part class AB implements A, B {
    public String a() { return ((B)this).z() + " " + ((A)this).z(); }
    public String b() { return ((A)this).z() + " " + ((B)this).z(); }
    public String z() { return "AB.z"; }
  }
  static @part class AImpl implements A {
    @link A a;
    AImpl(A a) { this.a = a; }
    public String z() { return "AImpl.z"; }
  }
  static @part class BImpl implements B {
    @link B b;
    BImpl(B b) { this.b = b; }
    public String z() { return "BImpl.z"; }
  }
}
