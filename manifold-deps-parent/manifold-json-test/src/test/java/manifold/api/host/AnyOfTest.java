package manifold.api.host;

import abc.AnyOfObj;
import abc.AnyOfList;
import junit.framework.TestCase;

/**
 */
public class AnyOfTest extends TestCase
{
  public void testAnyOfObj()
  {
    AnyOfObj obj = AnyOfObj.create();
    AnyOfObj.Option0 option0 = obj.asOption0();
    option0.setName( "fred" );
    assertEquals( "fred", option0.getName() );

    AnyOfObj.Option1 option1 = obj.asOption1();
    option1.setPoo( "poop" );
    assertEquals( "poop", option1.getPoo() );
  }

  public void testAnyOfList()
  {
    AnyOfList l = AnyOfList.create();
    AnyOfList.Option0 option0 = l.asOption0();
    option0.add( AnyOfList.Option0.Option0Item.create() );
    AnyOfList.Option0.Option0Item item = option0.get( 0 );
    item.setName( "mo" );
    assertEquals( "mo", item.getName() );
    AnyOfList list = (AnyOfList)option0;

    AnyOfList.Option1 option1 = AnyOfList.Option1.create();
    option1.add( AnyOfList.Option1.Option1Item.create() );
    AnyOfList.Option1.Option1Item item1 = option1.get( 0 );
    item1.setId( 11 );
    assertEquals( 11, item1.getId() );
    list = (AnyOfList)option1;
  }
}
