package extensions.java.util.List;

import manifold.test.api.ExtensionManifoldTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 */
public class ManifoldListCollectionsExtTest extends ExtensionManifoldTest {
  public void testCoverage() {
    testCoverage(ManifoldListCollectionExt.class);
  }

  public void testFirst() {
    List<String> list = makeTestList();
    assertEquals("scott", list.first());
    assertEquals("kyle", list.first(e -> e.contains("l")));
  }

  public void testFirstOrNull() {
    List<String> list = makeTestList();
    assertEquals("scott", list.firstOrNull());
    assertNull(empty().firstOrNull());
    assertEquals("kyle", list.firstOrNull(e -> e.contains("l")));
    assertNull(list.firstOrNull(e -> e.contains("z")));
  }

  public void testGetOrNull() {
    List<String> list = makeTestList();
    assertEquals("scott", list.getOrNull(0));
    assertNull(list.getOrNull(5));
    assertNull(empty().getOrNull(0));
  }

  public void testLast() {
    List<String> list = makeTestList();
    assertEquals("luca", list.last());
  }

  public void testLastOrNull() {
    List<String> list = makeTestList();
    assertEquals("luca", list.lastOrNull());
    assertNull(empty().lastOrNull());
  }

  public void testOptimizeReadOnlyList() {
    List<String> list = makeTestList();
    assertEquals(list, list.optimizeReadOnlyList());
    assertSame(Collections.emptyList(), empty().optimizeReadOnlyList());
  }

  public void testReverse() {
    List<String> list = makeTestList();
    list.reverse();
    assertEquals(Arrays.asList("luca", "carson", "kyle", "scott"), list);
  }

  public void testSingle() {
    List<String> list = makeTestList();
    try {
      list.single();
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    assertEquals( "one", Collections.singletonList("one").single() );

    try {
      empty().single();
      fail();
    } catch (NoSuchElementException e ) {
      // expected
    }
  }

  public void testSingleOrNull() {
    List<String> list = makeTestList();
    assertNull( list.singleOrNull() );
    assertEquals( "one", Collections.singletonList("one").singleOrNull() );
    assertNull( empty().singleOrNull() );
  }

  private List<String> empty() {
    return Collections.emptyList();
  }

  private List<String> makeTestList() {
    return Arrays.asList("scott", "kyle", "carson", "luca");
  }
}
