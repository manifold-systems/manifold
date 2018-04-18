package manifold.ext.producer.sample;

import java.util.ArrayList;
import junit.framework.TestCase;

import abc.stuff.Person;

public class SampleTest extends TestCase
{
  public void testSample()
  {
    String string = "myString";
    assertEquals( "Blue", string.favoriteColor() );
    assertEquals( "Pizza", string.favoriteFood() );
    assertEquals( "Jimmy Crack Corn", string.favoriteSong() );

    ArrayList<String> list = new ArrayList<>();
    assertEquals( "Red", list.favoriteColor() );
    assertEquals( "Pasta", list.favoriteFood() );
    assertEquals( "Redline", list.favoriteSong() );
  }

  // this test exercises ext-producer on a manifold type
  public void testWithLocalJson()
  {
    Person person = Person.create();
    assertEquals( "Chinese", person.favoriteFood() );
    assertEquals( "foobar", person.aMethod() );
  }
}
