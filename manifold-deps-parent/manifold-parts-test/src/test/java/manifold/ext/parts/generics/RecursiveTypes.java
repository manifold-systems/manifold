package manifold.ext.parts.generics;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class RecursiveTypes extends TestCase
{
  public void testRawRecursive()
  {
    RawBar rawBar = new RawBar( new BarPart<>());
    Foo foo = rawBar.defaultMethod( rawBar, rawBar );
    assertSame( foo, rawBar );
    assertEquals( "hi", rawBar.g( "hi") );
  }

  public void testDirectRecursive()
  {
    DirectBar directBar = new DirectBar();
    DirectBar result = directBar.defaultMethod( directBar, directBar );
    assertSame( result, directBar );
    assertEquals( "hi", directBar.g( "hi") );
  }
  
  public void testRawAbstractRecursive()
  {
    RawAbstractBar abstractBar = new RawAbstractBar();
    Foo result = abstractBar.defaultMethod( abstractBar, abstractBar );
    assertSame( result, abstractBar );
    assertEquals( "hi", abstractBar.g( "hi") );
    assertEquals( "hi", abstractBar.m().g( "hi") );
  }

  public void testDirectAbstractRecursive()
  {
    DirectAbstractBar abstractBar = new DirectAbstractBar();
    DirectAbstractBar result = abstractBar.defaultMethod( abstractBar, abstractBar );
    assertSame( result, abstractBar );
    assertEquals( "hi", abstractBar.g( "hi") );
    assertEquals( "hi", abstractBar.m().g( "hi") );
  }

  interface Foo<T extends Foo<T>> {
    T produce();
    default <TT extends T> T defaultMethod(T t, TT tt) {return t;}
  }
  interface Bar<B extends Bar<B>> extends Foo<B> {
    B m();
    <M> M g(M m);
  }
  static @part class BarPart<P extends Bar<P>> implements Bar<P> {
    @Override
    public P produce() {
      return (P) this;
    }

    @Override
    public P m() {
      return (P) this;
    }

    @Override
    public <MP> MP g(MP m) {
      return m;
    }
  }

  abstract @part class AbstractBarPart<P extends Bar<P>> implements Bar<P>
  {
    // implements none of Bar
  }

  static class RawBar implements Bar {
    @link Bar bar;
    RawBar( Bar bar) {
      this.bar = bar;
    }
  }

  static class DirectBar implements Bar<DirectBar> {
    @link Bar<DirectBar> bar = new BarPart<>();
  }

  static class RawAbstractBar implements Bar
  {
    @link Bar bar = AbstractBarPart.asLink();

    @Override
    public Bar m()
    {
      return this;
    }

    @Override
    public Object g( Object m )
    {
      return m;
    }

    @Override
    public Bar produce()
    {
      return this;
    }

// test the generation of this method, rather than manually overriding
//
//    @Override
//    public Foo defaultMethod( Foo bar, Foo tt )
//    {
//      return this.bar.defaultMethod( bar, tt );
//    }
  }

  static class DirectAbstractBar implements Bar<DirectAbstractBar>
  {
    @link Bar<DirectAbstractBar> bar = AbstractBarPart.asLink();

    @Override
    public DirectAbstractBar m()
    {
      return this;
    }

    @Override
    public <B> B g( B m )
    {
      return m;
    }

    @Override
    public DirectAbstractBar produce()
    {
      return this;
    }
// test the generation of this method, rather than manually overriding
//
//    @Override
//    public <TT extends DirectAbstractBar> DirectAbstractBar defaultMethod( DirectAbstractBar directAbstractBar, TT tt )
//    {
//      return this.bar.defaultMethod( directAbstractBar, tt );
//    }
  }
}

