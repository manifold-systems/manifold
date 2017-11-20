package manifold.api.host;

import abc.Contact;
import abc.Junk;
import abc.Outside;
import abc.Person;
import abc.Product;
import abc.Tree;
import abc.AllOf_Hierarchy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.Bindings;
import junit.framework.TestCase;


import static abc.Person.*;
import static abc.Person.Address.*;

/**
 */
public class JsonTest extends TestCase
{
  public void testAllOf()
  {
    AllOf_Hierarchy all = AllOf_Hierarchy.create();
    AllOf_Hierarchy.address address = AllOf_Hierarchy.address.create();
    address.setCity("Cupertino");
    all.setBilling_address(address);
    AllOf_Hierarchy.shipping_address shipping_address = AllOf_Hierarchy.shipping_address.create();
    shipping_address.setType("lol");
    all.setShipping_address(shipping_address);
    assertSame( shipping_address, all.getShipping_address() );
  }

  public void testRef( String[] args )
  {
    Junk junk = Junk.create();
    junk.setElem( Collections.singletonList( Junk.A.create() ) );
    Junk.A a = junk.getElem().get( 0 );
    assertNotNull( a );

    Junk.B b = Junk.B.create();
    b.setX( Junk.A.create() );
    b.getX().setFoo( "hi" );
    junk.setDing( Collections.singletonList( b ) );
    assertEquals( "hi", junk.getDing() );

    Outside.Alpha alpha = Outside.Alpha.create();
    Outside outside = Outside.create();
    outside.setGamma( Collections.singletonList( alpha ) );
    junk.setOutside( outside );
    Outside result = junk.getOutside();
    assertSame( result, outside );
  }

  public void testRecursion()
  {
    Tree tree = Tree.create();
    tree.setNode( Tree.node.create() );
    tree.getNode().setInfo( "root" );

    Tree child1 = Tree.create();
    child1.setNode( Tree.node.create() );
    child1.getNode().setInfo( "child1" );

    Tree child2 = Tree.create();
    child2.setNode( Tree.node.create() );
    child2.getNode().setInfo( "child2" );

    tree.setChildren( Arrays.asList( child1, child2 ) );
  }

  public void testJson()
  {
    Person person = Person.create();
    person.getAge();
    person.setName( "Joe Namath" );
    assertEquals( "Joe Namath", person.getName() );

    Address address = Address.create();
    address.setCity( "Dunedin" );
    person.setAddress( address );
    assertEquals( "Dunedin", person.getAddress().getCity() );

    Planet planet = Planet.create();
    address.setPlanet( planet );

    Hobby baseball = Hobby.create();
    baseball.setCategory( "Sport" );
    Hobby fishing = Hobby.create();
    fishing.setCategory( "Recreation" );
    List<Hobby> hobbies = asList( baseball, fishing );
    person.setHobby( hobbies );
    List<Hobby> h = person.getHobby();
    assertEquals( 2, h.size() );
    assertEquals( baseball, h.get( 0 ) );
    assertEquals( fishing, h.get( 1 ) );
  }

  public void testRef()
  {
    Contact contact = Contact.create();
    contact.getDateOfBirth();
    contact.setPrimaryAddress( Contact.Address.create() );
    Contact.Address primaryAddress = contact.getPrimaryAddress();
    primaryAddress.setStreet_address( "111 Foo Dr" );
    primaryAddress.setCity( "Cupertino" );
    primaryAddress.setState( "CA" );
    assertEquals( "111 Foo Dr", primaryAddress.getStreet_address() );
    assertEquals( "Cupertino", primaryAddress.getCity() );
    assertEquals( "CA", primaryAddress.getState() );
    assertEquals( "{\n" +
                  "  \"street_address\": \"111 Foo Dr\",\n" +
                  "  \"city\": \"Cupertino\",\n" +
                  "  \"state\": \"CA\"\n" +
                  "}", primaryAddress.toJson() );
  }

  public void testThing()
  {
    Product thing = Product.create();
    thing.setPrice( 1.55 );
    assertEquals( 1.55, thing.getPrice() );

    Product.dimensions dims = Product.dimensions.create();
    dims.setLength( 3.0 );
    dims.setWidth( 4.0 );
    dims.setHeight( 5.0 );
    thing.setDimensions( dims );
    Product.dimensions dims2 = thing.getDimensions();
    assertSame( dims, dims2 );

    Bindings bindings = (Bindings)thing;
    dims2 = (Product.dimensions)bindings.get( "dimensions" );
    assertSame( dims, dims2 );
  }

  public void testStructuralIntefaceCasting()
  {
    Person person = Person.create();
    IWhatever whatever = (IWhatever)person;
    assertEquals( "foobar", whatever.foobar() );
  }

  public void testToJsonXml()
  {
    Person person = Person.create();
    person.setName( "Joe Namath" );

    assertEquals( "{\n" +
                  "  \"Name\": \"Joe Namath\"\n" +
                  "}", person.toJson() );

    assertEquals( "<object>\n" +
                  "  <Name>Joe Namath</Name>\n" +
                  "</object>\n", person.toXml() );

    assertEquals( "<person>\n" +
                  "  <Name>Joe Namath</Name>\n" +
                  "</person>\n", person.toXml( "person" ) );
  }


  @SuppressWarnings("varargs")
  public static <T> List<T> asList( T... a )
  {
    return Arrays.asList( a );
  }

}
