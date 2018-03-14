package manifold.ext.producer.sample;

import java.util.ArrayList;
import junit.framework.TestCase;

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
}
