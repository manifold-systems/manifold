package manifold.js.demo;

import demo.ExtendedArrayList;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExtendingJavaTest {

  @Test
  @Ignore
  public void testBasicClassesWorks() {
    ExtendedArrayList extendedArrayList = new ExtendedArrayList();
    List asList = extendedArrayList;

    asList.add(1);  // Overwritten function add, prints what was added when called
    asList.add(2);
    asList.add(3);

    assertEquals(3, extendedArrayList.size());
    assertEquals(42, extendedArrayList.demo());
  }

}
