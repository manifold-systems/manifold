package manifold.util;

import manifold.util.concurrent.LocklessLazyVar;

import java.util.List;

public class DebugModeUtil
{
  private static LocklessLazyVar<Boolean> JDWP_ENABLED =
    new LocklessLazyVar<Boolean>()
    {
      protected Boolean init()
      {
        List<String> values = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        for( String value : values )
        {
          if( value.startsWith( "-Xrunjdwp:" ) ||
              value.startsWith( "-agentlib:jdwp=" ) )
          {
            return true;
          }
        }
        return false;
      }
    };

  public static boolean isJdwpEnabled()
  {
    return JDWP_ENABLED.get();
  }
}
