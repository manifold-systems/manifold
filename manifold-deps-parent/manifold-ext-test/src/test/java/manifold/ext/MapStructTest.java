package manifold.ext;

import java.util.HashMap;
import java.util.function.Function;
import junit.framework.TestCase;
import manifold.IMyStruct;
import manifold.ext.api.Structural;

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

  public void testCoercion()
  {
    HashMap<String, Object> map = new HashMap<>();
    IStuff mapStuff = (IStuff)map;

    mapStuff.set_double( 9.0 );
    assertEquals( 9.0, mapStuff.get_double() );
    assertEquals( 9.0, map.get( "double" ) );
    map.put( "double", 1 );
    assertEquals( 1, map.get( "double" ) );
    assertEquals( 1.0, mapStuff.get_double() );
    map.put( "double", "1" );
    assertEquals( "1", map.get( "double" ) );
    assertEquals( 1.0, mapStuff.get_double() );

    mapStuff.set_Double( 9.0 );
    assertEquals( 9.0, mapStuff.get_Double() );
    assertEquals( 9.0, map.get( "Double" ) );
    map.put( "Double", 1 );
    assertEquals( 1, map.get( "Double" ) );
    assertEquals( 1.0, mapStuff.get_Double() );
    map.put( "Double", "1" );
    assertEquals( "1", map.get( "Double" ) );
    assertEquals( 1.0, mapStuff.get_Double() );
  }

  @Structural
  interface IStuff
  {
    double get_double();
    void set_double( double d );
    Double get_Double();
    void set_Double( Double d );
    int get_int();
    Integer get_Integer();
  }

}
