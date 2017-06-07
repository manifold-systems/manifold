package extensions.java.lang.String;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class MyStringExt
{
  public static void echo( @This String thiz )
  {
    System.out.println( thiz );
  }

  public static boolean isAlpha( @This String thiz )
  {
    if( thiz == null )
    {
      return false;
    }
    int sz = thiz.length();
    for( int i = 0; i < sz; i++ )
    {
      if( !Character.isLetter( thiz.charAt( i ) ) )
      {
        return false;
      }
    }
    return true;
  }
}
