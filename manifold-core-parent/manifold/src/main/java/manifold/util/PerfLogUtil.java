package manifold.util;

import manifold.util.concurrent.LocklessLazyVar;

public class PerfLogUtil
{
  private static final LocklessLazyVar<Boolean> PERF =
    LocklessLazyVar.make( () -> {
      String value = System.getProperty( "manifold.perf", "false" );
      return Boolean.valueOf( value );
    } );

  public static void log( String label, Runnable run )
  {
    long before = System.nanoTime();
    try
    {
      run.run();
    }
    finally
    {
      log( label, before );
    }
  }

  public static void log( String label, long nanosBefore )
  {
    //noinspection ConstantConditions
    if( !PERF.get() )
    {
      return;
    }

    System.out.println( label + ": " + ((System.nanoTime() - nanosBefore) / 1_000_000) + "ms" );
  }
}
