package manifold.ext;

import java.util.HashMap;
import java.util.function.Function;
import junit.framework.TestCase;
import manifold.IMyStruct;

/**
 */
public class MapStructTest extends TestCase
{
  public void testMe()
  {
    HashMap<String, Object> map = new HashMap<>();
    IMyStruct mapStruct = (IMyStruct)map;

    mapStruct.setAge( 51 );
    assertEquals( 51, mapStruct.getAge() );

    map.put( "charAt", (Function<Integer, Character>)( i ) -> 'q' );
    assertEquals( 'q', mapStruct.charAt( 1 ) );
  }

  public void testActualName()
  {
    HashMap<String, Object> map = new HashMap<>();
    IMyStruct mapStruct = (IMyStruct)map;

    mapStruct.setName( "fred" );
    assertEquals( "fred", mapStruct.getName() );
    assertEquals( "fred", map.get( "name" ) );
    assertNull( map.get( "Name" ) );
  }

}
