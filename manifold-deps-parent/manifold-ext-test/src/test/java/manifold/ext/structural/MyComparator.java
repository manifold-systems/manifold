package manifold.ext.structural;

import java.util.Comparator;

public class MyComparator implements Comparator<Coordinate>
{
  @Override
  public int compare(Coordinate c1, Coordinate c2) {
    double res = c1.getX() == c2.getX() ? c1.getY() - c2.getY() : c1.getX() - c2.getX();
    return res < 0 ? -1 : res > 0 ? 1 : 0;
  }
}