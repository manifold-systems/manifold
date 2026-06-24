package manifold.ext.delegation;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class DefaultMethodChainTest extends TestCase
{
  // --- three-level default chain: f() -> g() -> h() (abstract), Root overrides h ---

  interface ThreeLevel
  {
    default String f() { return g(); }
    default String g() { return h(); }
    String h();
  }

  static @part class ThreeLevelPart implements ThreeLevel
  {
    @Override
    public String h() { return "part"; }
  }

  static class ThreeLevelRoot implements ThreeLevel
  {
    @link ThreeLevel tl = new ThreeLevelPart();

    @Override
    public String h() { return "root"; }
  }

  public void testThreeLevelChain()
  {
    assertEquals( "root", new ThreeLevelRoot().f() );
    assertEquals( "part", new ThreeLevelPart().f() );
  }

  // --- all-default chain: f -> g -> h; Root overrides middle (g), short-circuiting h ---

  interface AllDefault
  {
    default String f() { return g(); }
    default String g() { return h(); }
    default String h() { return "iface"; }
  }

  static @part class AllDefaultPart implements AllDefault {}

  static class MiddleOverrideRoot implements AllDefault
  {
    @link AllDefault ad = new AllDefaultPart();

    @Override
    public String g() { return "root-g"; }
  }

  public void testMiddleDefaultOverride()
  {
    assertEquals( "root-g", new MiddleOverrideRoot().f() );
    assertEquals( "iface", new AllDefaultPart().f() );
  }

  // --- default method returning `this` typed as interface — receiver is composite ---

  interface Fluent
  {
    default Fluent self() { return this; }
    String tag();
  }

  static @part class FluentPart implements Fluent
  {
    @Override
    public String tag() { return "part"; }
  }

  static class FluentRoot implements Fluent
  {
    @link Fluent fl = new FluentPart();

    @Override
    public String tag() { return "root"; }
  }

  public void testDefaultReturnsThis()
  {
    FluentRoot root = new FluentRoot();
    assertSame( root, root.self() );
    assertEquals( "root", root.self().tag() );

    FluentPart part = new FluentPart();
    assertSame( part, part.self() );
    assertEquals( "part", part.self().tag() );
  }

  // --- generic default: transform(t) calls wrap(t), Root overrides wrap ---

  interface Transformer<T>
  {
    default T transform( T t ) { return wrap( t ); }
    T wrap( T t );
  }

  static @part class TransformerPart<T> implements Transformer<T>
  {
    @Override
    public T wrap( T t ) { return t; }
  }

  static class TransformerRoot implements Transformer<String>
  {
    @link Transformer<String> tr = new TransformerPart<>();

    @Override
    public String wrap( String s ) { return "[" + s + "]"; }
  }

  public void testGenericDefaultDispatch()
  {
    assertEquals( "[hello]", new TransformerRoot().transform( "hello" ) );
    assertEquals( "hello", new TransformerPart<String>().transform( "hello" ) );
  }

  // --- default calls two abstract methods independently; Root overrides only one ---

  interface Combiner
  {
    default String combine() { return left() + right(); }
    String left();
    String right();
  }

  static @part class CombinerPart implements Combiner
  {
    @Override
    public String left() { return "L"; }

    @Override
    public String right() { return "R"; }
  }

  static class CombinerRoot implements Combiner
  {
    @link Combiner c = new CombinerPart();

    @Override
    public String left() { return "left-"; }
    // right() not overridden — part's "R" is used
  }

  public void testDefaultCallsMultiple()
  {
    assertEquals( "left-R", new CombinerRoot().combine() );
    assertEquals( "LR", new CombinerPart().combine() );
  }

  // --- default calls another default, which calls abstract; composite overrides abstract ---

  interface Decorator
  {
    default String decorate( String s ) { return wrap( s ); }
    default String wrap( String s ) { return "<" + raw( s ) + ">"; }
    String raw( String s );
  }

  static @part class DecoratorPart implements Decorator
  {
    @Override
    public String raw( String s ) { return s; }
  }

  static class DecoratorRoot implements Decorator
  {
    @link Decorator d = new DecoratorPart();

    @Override
    public String raw( String s ) { return s.toUpperCase(); }
  }

  public void testDefaultChainCallsAbstract()
  {
    assertEquals( "<HELLO>", new DecoratorRoot().decorate( "hello" ) );
    assertEquals( "<hello>", new DecoratorPart().decorate( "hello" ) );
  }

  // --- an intermediate @part forwards an inherited default to its delegate ---

  interface Named
  {
    default String label() { return "default"; }
    String tag();
  }

  static @part class OverridingLeaf implements Named
  {
    @Override public String label() { return "leaf"; }
    @Override public String tag()   { return "leaf"; }
  }

  static @part class ForwardingMiddle implements Named
  {
    @link Named inner = new OverridingLeaf();

    @Override public String tag() { return "middle"; }
  }

  static class NamedRoot implements Named
  {
    @link Named mid = new ForwardingMiddle();

    @Override public String tag() { return "root"; }
  }

  public void testDelegatingPartForwardsOverriddenDefault()
  {
    assertEquals( "leaf", new ForwardingMiddle().label() );
    assertEquals( "leaf", new NamedRoot().label() ); // forwards root -> middle -> leaf
  }
}
