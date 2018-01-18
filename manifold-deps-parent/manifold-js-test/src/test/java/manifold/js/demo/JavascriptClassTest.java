package manifold.js.demo;

import demo.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavascriptClassTest {

  @Test
  public void testBasicClassesWorks() {
    Person x = new Person("Joe", "Blow");
    Person y = new Person("Joe", "Blow");

    assertEquals("Joe", x.getFirstName());
    assertEquals("Blow", x.getLastName());
    assertEquals("Joe Blow", x.displayName());

    x.setFirstName("Henry");
    x.setLastName("Smith");

    assertEquals("Henry", x.getFirstName());
    assertEquals("Smith", x.getLastName());
    assertEquals("Henry Smith", x.displayName());


    assertEquals("Joe", y.getFirstName());
    assertEquals("Blow", y.getLastName());
    assertEquals("Joe Blow", y.displayName());

  }

}
