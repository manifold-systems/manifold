package manifold.ext.extensions.java.lang.String;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

/**
 */
@Extension
public class MyOtherStringExt
{
  public static void helloWorld( @This String thiz )
  {
    System.out.println( thiz );
  }
}
