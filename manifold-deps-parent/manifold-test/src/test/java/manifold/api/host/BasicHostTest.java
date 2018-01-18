package manifold.api.host;

import junit.framework.TestCase;
import manifold.internal.host.ManifoldHost;

/**
 */
public class BasicHostTest extends TestCase
{
  public void testBootstrap()
  {
    ManifoldHost.bootstrap();
  }
}
