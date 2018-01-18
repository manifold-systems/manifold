package manifold.ext.structural;

import java.awt.Point;
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
}
