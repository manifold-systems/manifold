package manifold.json.test.extensions.abc.Person;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class MyPersonExt
{
  public static String foobar( @This abc.Person thiz ) {
    return "foobar";
  }
}
