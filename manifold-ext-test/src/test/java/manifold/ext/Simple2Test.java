package manifold.ext;

import abc.IMyStructuralInterface;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class Simple2Test
{
  @Test
  public void testMe()
  {
    List<String> l2 = new ArrayList<>();
    assertSame( l2, l2.find( l2 ) );

    assertNotNull( def.foreverAlone_png.get() );
    assertEquals( 57, def.foreverAlone_png.get().width() );
    assertEquals( "kyle57", def.foreverAlone_png.get().myMethod( "kyle" ) );

    IMyStructuralInterface iface = def.foreverAlone_png.get();
    assertEquals( 57, iface.getIconWidth() );
    assertEquals( "lolwut57", iface.myMethod( "lolwut" ) );
  }

}
