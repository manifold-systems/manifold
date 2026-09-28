package manifold.ext.parts.parts.cycles;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.DelegationLinkageError;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class CycleDetectionTest extends TestCase
{
  public void testCycle()
  {
    try
    {
      new APart();
      fail( "Expecting cycle detection." );
    }
    catch( DelegationLinkageError dle )
    {
      assertTrue( dle.getMessage().startsWith( "Cycle detected" ) );
    }
  }

  interface A { void a(); }

  static @part class APart implements A
  {
    @link A a = new AAPart( this );
  }
  static @part class AAPart implements A
  {
    @link A a;

    AAPart( A a )
    {
      this.a = a;
    }
  }
}
