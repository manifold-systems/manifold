package manifold.api.host;

import java.util.Arrays;
import java.util.List;
import javax.script.SimpleBindings;
import junit.framework.TestCase;
import abc.Product;
import abc.Person;
import static abc.Person.*;

/**
 */
public class JsonTest extends TestCase
{
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

  public void testThing()
  {
    Product thing = Product.create();
    thing.setPrice( 1.55 );
    assertEquals( 1.55, thing.getPrice() );
  }

  @SuppressWarnings("varargs")
  public static <T> List<T> asList(T... a) {
      return Arrays.asList( a );
  }

}
