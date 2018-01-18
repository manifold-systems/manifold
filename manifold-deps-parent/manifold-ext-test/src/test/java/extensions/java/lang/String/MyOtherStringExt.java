package extensions.java.lang.String;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

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
