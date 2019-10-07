package manifold.ext.extensions.java.lang.String;

import java.util.List;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public abstract class MyStringExt
{
  public static void echo( @This String thiz )
  {
    System.out.println( thiz );
  }

  public static String times( @This String thiz, String that )
  {
    return thiz + that;
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

  /** for static ext method test */
  @Extension
  public static String valueOf( List list )
  {
    StringBuilder sb = new StringBuilder();
    for( int i = 0; i < list.size(); i++ )
    {
      Object o = list.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( o );
    }
    return sb.toString();
  }
}
