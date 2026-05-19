package manifold.ext.delegation.generics;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.internal;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class PolyCallStaticTypeParamTest extends TestCase
{
  public void test()
  {
    BarPart<Integer> bar = new BarPart<>();
    assertEquals( 2, bar.bar( 2 ).intValue() );
    assertEquals( 2, bar.bar( 2 ).intValue() );

    FooBar<Integer> fooBar = new FooBar<>();
    assertEquals( 10, fooBar.bar( 2 ).intValue() );
    assertEquals( 10, fooBar.bar2( 2 ).intValue() );
  }

  interface Foo<E> {
    E foo( E e );
  }
  interface Bar<E> extends Foo<String> {
    E bar( E e );
    E bar2( E e );
  }
  static @part class BarPart<E> implements Bar<E> {
    @Override
    public E bar( E e )
    {
      // polymorphic call to foo(), wo `this`
      return (E)Integer.valueOf( foo( e.toString() ) );
    }
    @Override
    public E bar2( E e )
    {
      // polymorphic call to foo(), with `this`
      return (E)Integer.valueOf( this.foo( e.toString() ) );
    }

    @Override
    public String foo( String e )
    {
      return e;
    }
  }
  static class FooBar<E> implements Bar<E>
  {
    @link Bar<E> bar = new BarPart<>();

    @Override
    public String foo( String s )
    {
      return "10";
    }
  }
}
