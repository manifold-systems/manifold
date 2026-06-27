package manifold.ext.delegation.parts.dag;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

/**
 * Tests arbitrary DAG topologies: deep chains, branching, intermediate composites.
 * Verifies that $selves[] is correctly propagated at each level.
 */
public class DagTopologyTest extends TestCase
{
  // --- three-level chain: Root -> PartMid -> PartLeaf ---
  // PartLeaf.invoke() calls method() via self-dispatch; $selves propagated 3 levels deep

  interface Chain
  {
    String invoke();
    String method();
  }

  static @part class PartLeaf implements Chain
  {
    @Override
    public String invoke() { return method(); } // self-dispatch

    @Override
    public String method() { return "leaf"; }
  }

  static @part class PartMid implements Chain
  {
    @link Chain inner = new PartLeaf();
    // method() not overridden - leaf handles it, but self-dispatch sees root
  }

  static class ChainRoot implements Chain
  {
    @link Chain mid = new PartMid();

    @Override
    public String method() { return "root"; }
  }

  public void testThreeLevelSelfDispatch()
  {
    assertEquals( "root", new ChainRoot().invoke() );   // $selves propagated to PartLeaf -> Root
    assertEquals( "leaf", new PartMid().invoke() );     // standalone mid -> leaf self-dispatch
    assertEquals( "leaf", new PartLeaf().invoke() );    // standalone leaf
  }

  // --- branching: Root links two independent parts; both dispatch shared method to Root ---

  interface Left
  {
    String leftResult();
    String shared();
  }

  interface Right
  {
    String rightResult();
    String shared();
  }

  static @part class PartLeft implements Left
  {
    @Override
    public String leftResult() { return "left:" + shared(); }

    @Override
    public String shared() { return "PartLeft.shared"; }
  }

  static @part class PartRight implements Right
  {
    @Override
    public String rightResult() { return "right:" + shared(); }

    @Override
    public String shared() { return "PartRight.shared"; }
  }

  static class BranchRoot implements Left, Right
  {
    @link Left left = new PartLeft();
    @link Right right = new PartRight();

    @Override
    public String shared() { return "root.shared"; }
  }

  public void testBranchingDispatch()
  {
    BranchRoot root = new BranchRoot();
    assertEquals( "left:root.shared", root.leftResult() );
    assertEquals( "right:root.shared", root.rightResult() );
  }

  // --- intermediate composite overrides a method; leaf's self-dispatch resolves to mid, not root ---
  // Root -> CalcMid (composite) -> CalcLeaf
  // CalcLeaf.compute() calls scale() via self-dispatch
  // CalcMid overrides scale(); Root does not
  // Self-dispatch from CalcLeaf should route through Root -> CalcMid.scale()

  interface Calc
  {
    int compute( int x );
    int scale( int x );
  }

  static @part class CalcLeaf implements Calc
  {
    @Override
    public int compute( int x ) { return scale( x ); } // self-dispatch

    @Override
    public int scale( int x ) { return x; }
  }

  static @part class CalcMid implements Calc
  {
    @link Calc leaf = new CalcLeaf();

    @Override
    public int scale( int x ) { return x * 10; }
  }

  static class CalcRoot implements Calc
  {
    @link Calc mid = new CalcMid();
    // scale() not overridden - CalcMid.scale() is visible
  }

  public void testIntermediateCompositeOverride()
  {
    assertEquals( 50, new CalcMid().compute( 5 ) );   // standalone mid: leaf dispatches to mid.scale
    assertEquals( 50, new CalcRoot().compute( 5 ) );  // through root: still routes to CalcMid.scale
  }

  // --- deep DAG with partial delegation at each level ---
  // Root links only some interfaces from PartOuter; PartOuter links all from PartInner
  // Internal methods of PartInner dispatch to PartOuter (its immediate delegator)

  interface Shape
  {
    String name();
    int sides();
  }

  interface Colored
  {
    String color();
  }

  static @part class InnerPart implements Shape, Colored
  {
    @Override
    public String name() { return "inner:" + color(); } // dispatches color() to self

    @Override
    public int sides() { return 0; }

    @Override
    public String color() { return "white"; }
  }

  static @part class OuterPart implements Shape, Colored
  {
    @link InnerPart inner = new InnerPart();

    @Override
    public String color() { return "blue"; } // OuterPart overrides color
  }

  static class PartialRoot implements Shape, Colored
  {
    @link(Shape.class) OuterPart outer = new OuterPart(); // only delegates Shape, not Colored

    @Override
    public String color() { return "red"; } // Root provides Colored directly
  }

  public void testPartialDelegationWithInternalDispatch()
  {
    // OuterPart standalone: name() -> color() dispatches to OuterPart.color() = "blue"
    assertEquals( "inner:blue", new OuterPart().name() );

    // PartialRoot: delegates Shape to OuterPart, Colored is provided by Root directly
    // InnerPart.name() calls color() - self-dispatch goes to OuterPart (inner's delegator)
    // OuterPart.color() = "blue" (OuterPart overrides it, and is InnerPart's composite)
    PartialRoot root = new PartialRoot();
    assertEquals( "inner:blue", root.name() );
    assertEquals( "red", root.color() ); // Root's own color, not delegated
  }
}
