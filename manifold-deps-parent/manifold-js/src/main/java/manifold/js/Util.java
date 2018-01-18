package manifold.js;

import java.util.concurrent.Callable;

public class Util
{
  static <T> T safe( Callable<T> elt )
  {
    try
    {
      return elt.call();
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
