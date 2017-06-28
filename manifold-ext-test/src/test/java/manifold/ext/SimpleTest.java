package manifold.ext;

import abc.Coordinate;
import abc.IGenericThing;
import java.awt.Rectangle;
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

    assertFalse( getClass().getName().isAlpha() );

    ArrayList<String> list = new ArrayList<>();
    list.add( "hi" );
    list.add( "hello" );
    assertEquals( 'h', list.first().charAt( 0 ) );

    String found = list.first( e -> e.length() > 2 );
    assertEquals( found, "hello" );

    assertEquals( "ok", list.stuff() );
    //assertEquals( "ok", list.stiff( "hi" ) );

    List<Serializable> l = Arrays.asList( "hi", 5 );
    assertEquals( 5, (Object)l.findByType( 3 ) );

    List<String> l2 = new ArrayList<>();
    assertEquals( Arrays.asList( "a", "b" ), l2.<String>append( "a", "b" ) );

    Rectangle rc = new Rectangle( 4, 5, 6, 7 );
    foo( (abc.Coordinate)rc );
    Coordinate coord = (Coordinate)rc;
    double d = coord.getX();
    System.out.println( d );
    System.out.println( coord.lol( 7 ) );
    IGenericThing<String> gthing = (IGenericThing)rc;
    List<String> glist = gthing.foo( "hi", new ArrayList<>() );
    System.out.println( glist.get( 0 ) );
  }

  public void testStructural()
  {
    ImplementStructurally is = new ImplementStructurally();
    Coordinate coord = (Coordinate)is;
    assertEquals( 1.0, coord.getX() );
    assertEquals( 2.0, coord.getY() );
    assertEquals( "lol3", coord.lol( 3 ) );

    // asserts object identity is not lost
    assertSame( coord, is );
  }

  private double foo( Coordinate c )
  {
    return c.getX();
  }

  public static class ImplementStructurally //implements Coordinate
  {
    public double getX()
    {
      return 1;
    }
    public double getY()
    {
      return 2;
    }
    public String lol( Integer i )
    {
      return "lol" + i;
    }
  }
}
