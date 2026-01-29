package manifold.ext;

import junit.framework.TestCase;
import manifold.ext.rt.api.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AutoTest extends TestCase
{
  public void testAuto()
  {
    assertTrue( AutoTest.has( String.class ) );
    auto cached = AutoTest.has( String.class );
    assertTrue( cached );

    assertTrue( AutoTest.has2( String.class ) );
    auto cached2 = AutoTest.has2( String.class );
    assertTrue( cached2 );

    auto maker = makeMaker();
    assertSame("hi", maker.apply( "hi" ));
  }

  public static <T> auto has( Class<T> clazz )
  {
    Map<Class<?>, String> instances = new HashMap<>();
    instances.put( String.class, clazz.getName() );
    return instances.containsKey( clazz );
  }

  public static <T> auto has2( Class<T> clazz )
  {
    Map<Class<?>, String> instances = new HashMap<>();
    instances.put( String.class, clazz.getName() );
    auto result = instances.containsKey( clazz );
    return result;
  }

  public static auto makeMaker()
  {
    auto result = AutoTest.maker();
    return result;
  }
  public static Function<String, ?> maker()
  {
    return s -> s;
  }

  public void testSimpleGenerics()
  {
    assertEquals( "hi", simple().get( 0 ) );
    auto list = returnsLub();
    String result = list.get( 0 );
    assertEquals( "hey", result );
  }

  public auto simple()
  {
    auto result = new LinkedList<String>();
    result.add( "hi" );
    return result;
  }
  public auto returnsLub()
  {
    auto result = new LinkedList<String>();
    result.add( "hi" );
    if( result.size() > 1 )
    {
      return result;
    }
    auto result2 = new ArrayList<String>();
    result2.add( "hey" );
    return result2;
  }

  public auto getNameForDisplay() {
    auto result = new LinkedList<String>();
    TestCase parent = this;
    do {
      auto name = parent.getName();
      result.addFirst((parent instanceof Runnable)
                      ? name
                      : (('<' + name) + '>'));
    } while (parent == null);

    return result;
  }

  public void testCompileTimeConstant()
  {
    auto x = 0;
    x = 3;
    assertEquals( 3, x );

    auto y = "hi";
    y = "bye";
    assertEquals( "bye", y );
  }

  public void testReturnsInsideLambdaDoNotInterfere()
  {
    auto result = hasReturnsInsideLambda();
    assertTrue( result );
  }

  // this method tests that return stmts inside a lambda do not interfere with resolving the auto return type
  private static auto hasReturnsInsideLambda()
  {
    auto a = true;
    if( a )
    {
      List<String> x = Arrays.asList( "a", "b", "c" );
      List<String> list = x.stream().map(s -> {
        if (s.length() == 1) {
          return s;
        }
        return s + s;
      }).collect( Collectors.toList() );
      return !list.isEmpty();
    }
    return false;
  }
}
