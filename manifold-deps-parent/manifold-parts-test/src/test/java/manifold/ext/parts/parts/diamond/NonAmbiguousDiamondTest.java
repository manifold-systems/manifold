package manifold.ext.parts.parts.diamond;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class NonAmbiguousDiamondTest extends TestCase
{
  /**
   * tests diamonds formed with single interface inheritance chain where the
   * composite directly links a superinterface of another link:
   * Base
   * | \
   * |  Sub
   * | /
   * Root
   *
   * Given interface Sub that extends Base,
   * Root links Sub and also links Base to override Sub link's impl of Base via delegation
   */
  public void testAutoResolve()
  {
    SubBase root = new Root_AutoResolve();
    String bye = root.bye();
    assertEquals( "BasePart.hi: Root : SubBasePart.bye", bye );
  }

  public void testExplicitResolve()
  {
    SubBase root = new Root_ExplicitResolve();
    String bye = root.bye();
    assertEquals( "BasePart.hi: Root : SubBasePart.bye", bye );
  }

  public void testAutoResolve_SubSubBase()
  {
    SubSubBase root = new Root_AutoResolveSubSubBase();
    String bye = root.fairwell();
    assertEquals( "BasePart.hi: Root : SubBasePart.bye : SubSubBasePart.fairwell", bye );
  }

  interface Base
  {
    String hi();
  }
  interface SubBase extends Base
  {
    String bye();
  }
  interface SubSubBase extends SubBase
  {
    String fairwell();
  }

  static @part class BasePart implements Base
  {
    private final String hi;

    public BasePart( String hi )
    {
      this.hi = hi;
    }

    @Override
    public String hi()
    {
      return "BasePart.hi: " + hi;
    }
  }

  static @part class SubBasePart implements SubBase
  {
    private final String bye;

    public SubBasePart( String bye )
    {
      this.bye = bye;
    }
    public SubBasePart()
    {
      this( "" );
    }

    @Override
    public String hi()
    {
      return "SubBasePart.hi";
    }

    @Override
    public String bye()
    {
      return hi() + " : " + "SubBasePart.bye";
    }
  }

  static @part class SubSubBasePart implements SubSubBase
  {
    private final String fairwell;

    public SubSubBasePart( String fairwell )
    {
      this.fairwell = fairwell;
    }

    @Override
    public String hi()
    {
      return "SubSubBasePart.hi";
    }

    @Override
    public String bye()
    {
      return hi() + " : " + "SubSubBasePart.bye";
    }

    @Override
    public String fairwell()
    {
      return bye() + " : " + "SubSubBasePart.fairwell";
    }
  }

  static class Root_AutoResolve implements SubBase
  {
    @link SubBase _subBase = new SubBasePart();
    @link Base _base = new BasePart( "Root");
  }

  static class Root_ExplicitResolve implements SubBase
  {
    @link SubBase _subBase = new SubBasePart();
    @link(share=Base.class) Base _base = new BasePart( "Root");
  }

  static class Root_AutoResolveSubSubBase implements SubSubBase
  {
    @link SubSubBase _subSubBase = new SubSubBasePart( "Root" );
    @link SubBase _subBase = new SubBasePart( "Root" );
    @link Base _base = new BasePart( "Root" );
  }
}
