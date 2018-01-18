package extensions.abc.benis_png;

import abc.IMyStructuralInterface;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

import abc.benis_png;

/**
 */
@Extension
public abstract class MyBenis_pngExt implements IMyStructuralInterface
{
  public static int width( @This abc.benis_png thiz )
  {
    return thiz.getIconWidth();
  }

  public static String myMethod( @This benis_png thiz, String stringParam )
  {
    return stringParam + thiz.width();
  }

  // test warning for missing @This
  public static String myMethod2( benis_png thiz, String stringParam )
  {
    return stringParam + thiz.width();
  }
}
