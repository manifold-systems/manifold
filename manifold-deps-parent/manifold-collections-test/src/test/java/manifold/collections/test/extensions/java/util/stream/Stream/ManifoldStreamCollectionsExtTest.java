package manifold.collections.test.extensions.java.util.stream.Stream;

import manifold.collections.extensions.java.util.stream.Stream.ManifoldStreamCollectionsExt;
import manifold.test.api.ExtensionManifoldTest;

import java.util.*;
import java.util.stream.Stream;

public class ManifoldStreamCollectionsExtTest extends ExtensionManifoldTest {

  @Override
  public void testCoverage() {
    testCoverage(ManifoldStreamCollectionsExt.class);
  }

  public void testGroupingBy() {
    Map<Integer, List<String>> stringsByLength = stream(Arrays.asList("a", "b", "aa", "ab", "abc")).groupingBy(String::length);
    assertEquals(2, stringsByLength.get(1).size());
    assertEquals(2, stringsByLength.get(2).size());
    assertEquals(1, stringsByLength.get(3).size());
    assertEquals(null, stringsByLength.get(4));
  }

  public void testToList() {
    List<String> sampleList = makeTestList();
    assertEquals(sampleList, stream(sampleList).toList());
  }

  public void testToSet() {
    List<String> sampleList = makeTestList();
    assertEquals(new HashSet<>(stream(sampleList).toList()), stream(sampleList).toSet());
  }

  public void testToMap() {
    List<String> sampleList = makeTestList();
    Map<Integer, String> byLen = stream(sampleList).toMap(String::length);
    assertEquals("a", byLen.get(1));
    assertEquals("aaa", byLen.get(3));

    Map<Integer, Integer> hashCodeByLen = stream(sampleList).toMap(String::length, String::hashCode);
    assertEquals((Integer) "a".hashCode(), hashCodeByLen.get(1));
    assertEquals((Integer) "aaa".hashCode(), hashCodeByLen.get(3));
  }

  private List<String> makeTestList() {
    return Arrays.asList("a", "aa", "aaa");
  }

  private <T> Stream<T> stream(Collection<T> c) {
    return c.stream();
  }
}
