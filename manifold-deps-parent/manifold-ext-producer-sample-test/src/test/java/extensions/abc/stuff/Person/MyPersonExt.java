package extensions.abc.stuff.Person;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class MyPersonExt
{
  public static String aMethod( @This abc.stuff.Person thiz ) {
    return "foobar";
  }
}
