package manifold.util;

import java.util.List;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class BytecodeOptions
{
  public static LocklessLazyVar<Boolean> JDWP_ENABLED =
    new LocklessLazyVar<Boolean>() {
      protected Boolean init() {
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
}
