package manifold.ext.extensions.java.awt.geom.RectangularShape;

import abc.Coordinate;
import abc.IGenericThing;
import java.awt.geom.RectangularShape;
import java.util.List;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public abstract class MyRectangularShapeExtension implements IGenericThing, Coordinate
{
  public static String lol( @This RectangularShape thiz, Integer i )
  {
    return String.valueOf( i + " : " + thiz );
  }

  public static List foo( @This RectangularShape thiz, CharSequence t, List e )
  {
    e.add( t );
    return e;
  }
}
