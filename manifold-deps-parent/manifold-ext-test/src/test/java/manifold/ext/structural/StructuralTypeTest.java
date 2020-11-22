package manifold.ext.structural;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;

/**
 */
public class StructuralTypeTest extends TestCase
{
  public void testTopLevelSort()
  {
    Comparator<? super Coordinate> coordSorter = new MyComparator();
    List<Point> points = makeSampleList();
    Collections.sort( (List)points, coordSorter );
    assertEquals( makeSampleList_Sorted(), points );
  }

  public void testInnerClassSort()
  {
    Comparator<? super Coordinate> coordSorter = new Comp();
    List<? extends Coordinate>  points = (List)makeSampleList();
    Collections.sort( points, coordSorter );
    assertEquals( makeSampleList_Sorted(), points );
  }

  public void testAonymousClassSort()
  {
    Comparator<? super Coordinate> coordSorter = new Comparator<Coordinate>() {
      @Override
      public int compare(Coordinate c1, Coordinate c2) {
        double res = c1.getX() == c2.getX() ? c1.getY() - c2.getY() : c1.getX() - c2.getX();
        return res < 0 ? -1 : res > 0 ? 1 : 0;
      }
    };
    List<? extends Coordinate> points = (List)makeSampleList();
    Collections.sort( points, coordSorter );
    assertEquals( makeSampleList_Sorted(), points );
  }

  public void testLambdaSort()
  {
    Comparator<? super Coordinate> coordSorter = (c1, c2) ->
          {
            double res = c1.getX() == c2.getX() ? c1.getY() - c2.getY() : c1.getX() - c2.getX();
            return res < 0 ? -1 : res > 0 ? 1 : 0;
          };
    List<? extends Coordinate> points = (List)makeSampleList();
    Collections.sort( points, coordSorter );
    assertEquals( makeSampleList_Sorted(), points );
  }

  public void testCallFromBoundedTypeVar()
  {
    Rectangle rectangle = new Rectangle( 1, 2, 3, 4 );
    HasCoordinate<Rectangle> rectCoord = new HasCoordinate<>( rectangle );
    assertEquals( 1.0, rectCoord.getX() );
  }

  public void testEqualsUsesRawValues()
  {
    Coordinate c1 = (Coordinate)new Point( 8, 9 );
    Coordinate c2 = (Coordinate)new Point( 8, 9 );

    boolean equals = c1.equals( c2 );
    assertTrue( equals );
    equals = c2.equals( c1 );
    assertTrue( equals );
  }

  public void testHashCodeUsesRawValues()
  {
    Point point = new Point( 8, 9 );
    Coordinate coord = (Coordinate)point;
    assertEquals( point.hashCode(), coord.hashCode() );
  }

  // tests that if a ternary expression's type is structural that it is transformed properly during
  // manifold transformation/generation
  public void testTernary()
  {
    Point point = new Point( 8, 9 );
    Coordinate c = (Coordinate)new Point( 1, 2 );

    Coordinate pt = point.x > 0
      // Cast is significant here as it is erased during transformation, bc during generation the ternary expr type is
      // determined from the least upper bound of the ternary branch expressions.
      ? (Coordinate)point
      : c;
    assertSame( (Coordinate)point, pt );

    pt = point.x > 0 ?
      c:
      (Coordinate)point;
    assertSame( c, pt );
  }

  public void testGetClassUsesRawValues()
  {
    Coordinate coord = (Coordinate) new Point( 8, 9 );
    assertEquals( Point.class, coord.getClass() );
  }

  private List<Point> makeSampleList()
  {
    return Arrays.asList( new Point( 3, 2 ), new Point( 1, 2 ), new Point( 3, 5 ), new Point( 1, 1 ) );
  }
  private List<Point> makeSampleList_Sorted()
  {
    return Arrays.asList( new Point( 1, 1 ), new Point( 1, 2 ), new Point( 3, 2 ), new Point( 3, 5 ) );
  }

  static class Comp implements Comparator<Coordinate>
  {
    @Override
    public int compare(Coordinate c1, Coordinate c2) {
      double res = c1.getX() == c2.getX() ? c1.getY() - c2.getY() : c1.getX() - c2.getX();
      return res < 0 ? -1 : res > 0 ? 1 : 0;
    }
  }

  static class HasCoordinate<E extends abc.Coordinate>
  {
    private final E _coord;

    HasCoordinate( E coord )
    {
      _coord = coord;
    }

    double getX()
    {
      return _coord.getX();
    }
  }
}
