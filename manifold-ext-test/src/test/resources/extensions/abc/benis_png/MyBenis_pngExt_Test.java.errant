package extensions.abc.benis_png;

import abc.IMyStructuralInterface;
import abc.benis_png;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public abstract class MyBenis_pngExt_Test
{
  // compile error for method duplication
  public static int getIconWidth( @This benis_png thiz ) {
    return 0;
  }

  // compile warning for missing @This
  public static String myMethod2( benis_png thiz, String stringParam )
  {
    return stringParam + thiz.width();
  }
}
