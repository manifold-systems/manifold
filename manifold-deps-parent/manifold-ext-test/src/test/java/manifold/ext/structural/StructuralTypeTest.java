/*
 * Copyright (c) 2021 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext.structural;

import java.awt.Point;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;

import junit.framework.TestCase;
import manifold.ext.rt.api.Structural;

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
    // tests structural assignability in type parameters: Rectangle to Coordinate
    HasCoordinate<Rectangle> rectCoord = new HasCoordinate<>( rectangle );
    assertEquals( 1.0, rectCoord.getX() );
  }

  public void testStructuralAssignability()
  {
    NotTestIface notTestIface = new NotTestIfaceImpl();
    // tests type checked structural assignment
    TestIface testIface = notTestIface;
    // tests structural call to method with covariance in return type and contravariance in argument types
    CharSequence result = testIface.foo( 2, "hi", new ArrayList<String>(){{add("hi");}} );
    assertEquals( "hi2.0hi", (String)result );
    LocalDateTime dateTime1 = LocalDateTime.of( 1987, 6, 17, 23, 30 );
    // tests structural call to method with covariance in return type and contravariance in argument types with generics
    ChronoLocalDateTime<LocalDate> dateTimeResult = testIface.bar( 2, dateTime1, new ArrayList<String>(){{add("hi");}} );
    assertEquals( dateTime1, dateTimeResult );
  }

  public void testStructuralAssignabilityGenerics()
  {
    NotCoordinate notCoordinate = new NotCoordinateImpl();
    // tests type checked structural assignment
    Coordinate coord = notCoordinate;
    assertEquals( 1d, coord.getX() );
    // tests structural assignability in type parameters: NotCoordinate to Coordinate
    HasCoordinate<NotCoordinate> notCoord = new HasCoordinate<>( notCoordinate ); // no cast required
    assertEquals( 1d, notCoord.getX() );
  }

  public static class NotCoordinateImpl implements NotCoordinate
  {
    @Override
    public double getX() {
      return 1;
    }

    @Override
    public double getY() {
      return 2;
    }

    @Override
    public String lol(Integer i) {
      return null;
    }
  }

  interface NotCoordinate
  {
    double getX();
    double getY();
    String lol( Integer i );
  }

  public static class NotTestIfaceImpl extends SuperNotTestIfaceImpl implements NotTestIface
  {
    @Override
    public String foo( double d, CharSequence c, List<String> list )
    {
      return c.toString() + d + list.get( 0 );
    }
  }
  public static class SuperNotTestIfaceImpl implements SuperNotTestIface
  {
    @Override
    public LocalDateTime bar( int i, ChronoLocalDateTime<LocalDate> dateTime, List<String> list )
    {
      return (LocalDateTime)dateTime;
    }
  }
  public interface NotTestIface extends SuperNotTestIface
  {
    String foo( double d, CharSequence c, List<String> list );
  }
  public interface SuperNotTestIface
  {
    LocalDateTime bar( int i, ChronoLocalDateTime<LocalDate> dateTime, List<String> list );
  }
  @Structural
  public interface TestIface extends SuperTestIface
  {
    CharSequence foo( int i, String s, List<String> list );
  }
  public interface SuperTestIface
  {
    ChronoLocalDateTime<LocalDate> bar( int i, LocalDateTime dateTime, List<String> list );
  }

  public void testAnonymousClass()
  {
    Coordinate c = (Coordinate) makeAnonymousClass();
    assertEquals( 1.0, c.getX() );
    assertEquals( 2.0, c.getY() );
  }
  public Runnable makeAnonymousClass()
  {
    return new Runnable() {
      public void run() {}
      public double getX() { return 1.0; }
      public double getY() { return 2.0; }
    };
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

  public void testFinalClass()
  {
    Bob bob = (Bob)new BobImpl();
    bob.bob();
  }
  @Structural
  interface Bob {
    void bob();
  }
  static final class BobImpl {
    public void bob() {}
  }
}
