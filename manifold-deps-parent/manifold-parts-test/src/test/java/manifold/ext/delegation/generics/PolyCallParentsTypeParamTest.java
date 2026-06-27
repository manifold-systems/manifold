package manifold.ext.delegation.generics;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class PolyCallParentsTypeParamTest extends TestCase
{
  public void test()
  {
    BarPart<Integer> bar = new BarPart<>();
    assertEquals( 2, bar.bar( 2 ).intValue() );
    assertEquals( 2, bar.bar( 2 ).intValue() );

    FooBar<Integer> fooBar = new FooBar<>();
    assertEquals( 10, fooBar.bar( 2 ).intValue() );
    assertEquals( 10, fooBar.bar2( 2 ).intValue() );
    assertEquals( 20, fooBar.bar3( 2 ).intValue() );
  }
  interface Foo<E> {
    E foo( E e );
    <T extends Number> T goo( E e, T t );
  }
  interface Bar<E extends Number> extends Foo<E> {
    E bar( E e );
    E bar2( E e );
    E bar3( E e );
  }
  static @part class BarPart<E extends Number> implements Bar<E> {
    @Override
    public E bar( E e )
    {
      // polymorphic call to foo(), wo `this`
      return foo( e );
    }
    @Override
    public E bar2( E e )
    {
      // polymorphic call to foo(), with `this`
      return this.foo( e );
    }
    @Override
    public E bar3( E e )
    {
      // polymorphic call to generic goo()
      return this.goo( e, (E)(Integer)10 );
    }

    @Override
    public E foo( E e )
    {
      return e;
    }

    @Override
    public <T extends Number> T goo( E e, T t )
    {
      return t;
    }
  }
  static class FooBar<E extends Number> implements Bar<E>
  {
    @link Bar<E> bar = new BarPart<>();

    @Override
    public E foo( E e )
    {
      return (E)Integer.valueOf( 10 );
    }

    @Override
    public <T extends Number> T goo( E e, T t )
    {
      return (T)(Integer)((Integer)e * (Integer)t);
    }
  }
}
