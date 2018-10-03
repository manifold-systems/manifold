package manifold.api.host;

import junit.framework.TestCase;
import manifold.internal.host.RuntimeManifoldHost;

/**
 */
public class BasicHostTest extends TestCase
{
  public void testBootstrap()
  {
    RuntimeManifoldHost.bootstrap();
  }
}
