package manifold.api.host;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import abc.impl.Person;
import static abc.impl.Person.*;

import abc.impl.Product;

/**
 */
public class JsonTest extends TestCase
{
  public void testJson()
  {
    Person person = new Person();
    person.getAge();
    person.setName( "Joe Namath" );
    assertEquals( "Joe Namath", person.getName() );

    Address address = new Address();
    address.setCity( "Dunedin" );
    person.setAddress( address );
    assertEquals( "Dunedin", person.getAddress().getCity() );

    Hobby baseball = new Hobby();
    baseball.setCategory( "Sport" );
    Hobby fishing = new Hobby();
    fishing.setCategory( "Recreation" );
    List<Hobby> hobbies = Arrays.asList( baseball, fishing );
    person.setHobby( hobbies );
    List<Hobby> h = person.getHobby();
    assertEquals( 2, h.size() );
    assertEquals( baseball, h.get( 0 ) );
    assertEquals( fishing, h.get( 1 ) );
  }

  public void testThing()
  {
    Product thing = new Product();
    thing.setPrice( 1.55 );
    assertEquals( 1.55, thing.getPrice() );
  }
}
