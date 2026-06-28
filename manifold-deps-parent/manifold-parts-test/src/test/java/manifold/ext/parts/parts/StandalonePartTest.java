package manifold.ext.parts.parts;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

/**
 * Verifies that @part classes work correctly in isolation. $selves[] is pre-filled
 * with `this`, so self-dispatch resolves correctly without any composite.
 */
public class StandalonePartTest extends TestCase
{
  // --- standalone @part: default calling abstract via self-dispatch ---

  interface MathOps
  {
    default int addThenDouble( int a, int b ) { return doDouble( add( a, b ) ); }
    int add( int a, int b );
    int doDouble( int n );
  }

  static @part class MathPart implements MathOps
  {
    @Override
    public int add( int a, int b ) { return a + b; }

    @Override
    public int doDouble( int n ) { return n * 2; }
  }

  public void testStandalonePartSelfDispatch()
  {
    MathPart part = new MathPart();
    assertEquals( 6, part.addThenDouble( 1, 2 ) ); // (1+2)*2=6, $selves pre-filled with `this`
  }

  // --- standalone sub-composite: part with its own @link works standalone ---

  interface Formatter
  {
    String format( String s );
  }

  interface Processor
  {
    String process( String s );
  }

  static @part class FormatterPart implements Formatter
  {
    @Override
    public String format( String s ) { return "[" + s + "]"; }
  }

  static @part class ProcessorPart implements Processor, Formatter
  {
    @link Formatter fmt = new FormatterPart();

    @Override
    public String process( String s ) { return format( s.toUpperCase() ); }
  }

  public void testStandaloneSubComposite()
  {
    // ProcessorPart links FormatterPart; used standalone without an outer composite
    ProcessorPart part = new ProcessorPart();
    assertEquals( "[HELLO]", part.process( "hello" ) );
  }

  // --- when composed, the composite's override is visible via self-dispatch ---

  static class ComposedProcessor implements Processor, Formatter
  {
    @link ProcessorPart pp = new ProcessorPart();

    @Override
    public String format( String s ) { return "{" + s + "}"; } // overrides format
  }

  public void testComposedPartSeesOverride()
  {
    ComposedProcessor composed = new ComposedProcessor();
    assertEquals( "{HELLO}", composed.process( "hello" ) );
  }

  // --- standalone part with self-referential default method ---

  interface Builder
  {
    default String build() { return prefix() + body() + suffix(); }
    String prefix();
    String body();
    String suffix();
  }

  static @part class BuilderPart implements Builder
  {
    @Override
    public String prefix() { return "("; }

    @Override
    public String body() { return "content"; }

    @Override
    public String suffix() { return ")"; }
  }

  static class CustomBuilder implements Builder
  {
    @link Builder b = new BuilderPart();

    @Override
    public String prefix() { return "<<"; }

    @Override
    public String suffix() { return ">>"; }
    // body() not overridden - uses part's "content"
  }

  public void testStandaloneVsComposed()
  {
    assertEquals( "(content)", new BuilderPart().build() );
    assertEquals( "<<content>>", new CustomBuilder().build() );
  }

  // --- standalone part hierarchy (part extends part) works via self-dispatch ---

  interface Greeter
  {
    default String greet() { return "Hello, " + name() + "!"; }
    String name();
  }

  static @part class BaseGreeter implements Greeter
  {
    @Override
    public String name() { return "World"; }
  }

  static @part class SpecialGreeter extends BaseGreeter
  {
    @Override
    public String name() { return "Friend"; }
  }

  static class ComposedGreeter implements Greeter
  {
    @link Greeter g = new SpecialGreeter();

    @Override
    public String name() { return "Root"; }
  }

  public void testStandalonePartHierarchy()
  {
    assertEquals( "Hello, World!", new BaseGreeter().greet() );
    assertEquals( "Hello, Friend!", new SpecialGreeter().greet() );
    assertEquals( "Hello, Root!", new ComposedGreeter().greet() );
  }
}
