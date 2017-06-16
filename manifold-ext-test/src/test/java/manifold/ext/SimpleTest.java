package manifold.ext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 */
public class SimpleTest extends TestCase
{
  public void testMe()
  {
    "this is impossible".echo();
    "this is impossible".helloWorld();

    ArrayList<String> lsist = new ArrayList<>( Arrays.asList( "hi", "and", "hello" ) );

    assertFalse( getClass().getName().isAlpha() );

    ArrayList<String> list = new ArrayList<>();
    list.add( "hi" );
    list.add( "hello" );
    assertEquals( 'h', list.first().charAt( 0 ) );

    String found = list.first( e -> e.length() > 2 );
    assertEquals( found, "hello" );

    assertEquals( "ok", list.stuff() );

    List<Serializable> l = Arrays.asList( "hi", 5 );
    assertEquals( 5, (Object)l.findByType( 3 ) );

    List<String> l2 = new ArrayList<>();
    assertEquals( Arrays.asList( "a", "b" ), l2.<String>append( "a", "b" ) );
  }

}
