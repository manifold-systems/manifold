package extensions.def.foreverAlone_png;

import abc.IMyStructuralInterface;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

import def.foreverAlone_png;

@Extension
public abstract class ForeverAlone_pngExt implements IMyStructuralInterface {

  public static int width( @This foreverAlone_png thiz )
  {
    return thiz.getIconWidth();
  }

  public static String myMethod( @This foreverAlone_png thiz, String stringParam )
  {
    return stringParam + thiz.width();
  }

}
