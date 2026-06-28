package manifold.ext.parts.annotations.internal;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.internal;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class InternalLinkFieldAccessTest extends TestCase
{
  // --- link field is the "super analog": composite can call @internal methods via link field ---

  interface Protocol
  {
    String result();
    @internal String step();
  }

  static @part class ProtocolPart implements Protocol
  {
    @Override
    public String result() { return step() + "-done"; }

    @Override
    public String step() { return "step"; }
  }

  static class ProtocolRoot implements Protocol
  {
    @link Protocol proto = new ProtocolPart();

    public String callInternalViaLink()
    {
      return proto.step(); // link field gives access to @internal
    }
  }

  public void testLinkFieldAccessToInternal()
  {
    ProtocolRoot root = new ProtocolRoot();
    assertEquals( "step-done", root.result() );
    assertEquals( "step", root.callInternalViaLink() );
  }

  // --- composite overrides the @internal method; part sees override via self-dispatch ---

  static class ProtocolRootOverride implements Protocol
  {
    @link Protocol proto = new ProtocolPart();

    @Override
    public String step() { return "overridden-step"; }
  }

  public void testCompositeOverridesInternal()
  {
    ProtocolRootOverride root = new ProtocolRootOverride();
    // result() calls step() via self-dispatch; composite's step() is visible
    assertEquals( "overridden-step-done", root.result() );
  }

  // --- @internal method auto-inherited: subpart override treated as @internal without re-annotation ---

  static @part class SubProtocolPart extends ProtocolPart
  {
    @Override
    public String step()
    {
      // no @internal here — inherited from interface via ProtocolPart
      return "sub-step";
    }
  }

  static class SubProtocolRoot implements Protocol
  {
    @link Protocol proto = new SubProtocolPart();

    public String callInternalViaLink()
    {
      return proto.step();
    }
  }

  public void testInternalInheritedInSubpart()
  {
    SubProtocolRoot root = new SubProtocolRoot();
    assertEquals( "sub-step-done", root.result() );
    assertEquals( "sub-step", root.callInternalViaLink() );
  }

  // --- @internal default method cooperates between part and composite via self-dispatch ---

  interface Coordinator
  {
    String publicOutput();

    @internal
    default String coordinate() { return "coordinated:" + signal(); }

    @internal String signal();
  }

  static @part class CoordinatorPart implements Coordinator
  {
    @Override
    public String publicOutput()
    {
      return coordinate(); // calls @internal default, which calls signal() via self-dispatch
    }

    @Override
    public String signal() { return "base-signal"; }
  }

  static class CoordinatorRoot implements Coordinator
  {
    @link Coordinator coord = new CoordinatorPart();

    @Override
    public String signal() { return "root-signal"; } // composite overrides @internal
  }

  public void testInternalDefaultCooperation()
  {
    CoordinatorRoot root = new CoordinatorRoot();
    // publicOutput() -> coordinate() [default] -> signal() [overridden in Root via self-dispatch]
    assertEquals( "coordinated:root-signal", root.publicOutput() );

    // standalone part uses its own signal()
    CoordinatorPart part = new CoordinatorPart();
    assertEquals( "coordinated:base-signal", part.publicOutput() );
  }

  // --- @internal interface + @internal method combination: cooperation without leakage ---

  interface Engine
  {
    String run();
    @internal String power();
  }

  interface Vehicle
  {
    String drive();
  }

  static @part class EnginePart implements Engine, @internal Vehicle
  {
    @Override
    public String run() { return "engine:" + power(); }

    @Override
    public String power() { return "100hp"; }

    @Override
    public String drive() { return "EnginePart.drive"; }
  }

  static class Car implements Engine
  {
    @link Engine engine = new EnginePart();

    public String powerViaLink() { return engine.power(); } // @internal access via link field
  }

  public void testInternalMethodAndInterface()
  {
    Car car = new Car();
    assertEquals( "engine:100hp", car.run() );
    assertEquals( "100hp", car.powerViaLink() ); // link field access to @internal method
    // Car does not implement Vehicle — @internal blocks delegation of Vehicle
  }
}
