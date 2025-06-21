package manifold.ext.params;

import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodRefTest extends TestCase
{
  public void testConstructor()
  {
    List<Foo> list = Stream.of( 1, 2 ).map( Foo::new ).collect( Collectors.toList() );
    assertEquals( 2, list.size() );
    Foo foo0 = list.get( 0 );
    assertEquals( (bar:1, test:""), (foo0.bar, foo0.test) );
    Foo foo1 = list.get( 1 );
    assertEquals( (bar:2, test:""), (foo1.bar, foo1.test) );
  }

  public static class Foo
  {
    int bar;
    String test;

    public Foo( int bar, String test="" )
    {
      this.bar = bar;
      this.test = test;
    }

  }
}
