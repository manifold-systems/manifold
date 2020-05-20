package extensions.abc.stuff.Person;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

/**
 */
@Extension
public class MyPersonExt
{
  public static String aMethod( @This abc.stuff.Person thiz ) {
    return "foobar";
  }
}
