package extensions.abc.benis_png;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

import abc.benis_png;

/**
 */
@Extension
public class MyBenis_pngExt
{
  public static int width( @This abc.benis_png thiz )
  {
    return thiz.get().getIconWidth();
  }

  public static String myMethod( @This benis_png thiz, String stringParam )
  {
    return stringParam + thiz.width();
  }
}
