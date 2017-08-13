package extensions.java.util.Collection;

//import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import manifold.api.ExtensionManifoldTest;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class ManifoldCollectionExtTest extends ExtensionManifoldTest {
  @Override
  public void testCoverage() {
    testCoverage(ManifoldCollectionExt.class);
  }

  public void testAddAll() {
    List<String> c = new ArrayList<>(makeTestCollection());
    c.addAll(makeTestIterable());
    List<String> expected = new ArrayList<>(makeTestCollection());
    expected.addAll((List<String>) makeTestIterable());
    assertEquals(expected, c);
  }

  public void testAllMatch() {
    List<String> c = makeTestCollection();
    assertTrue(c.allMatch(e -> e.length() > 3));
    assertFalse(c.allMatch(e -> e.length() > 4));
  }

  public void testAnyMatch() {
    List<String> c = makeTestCollection();
    assertTrue(c.anyMatch(e -> e.length() > 5));
    assertFalse(c.anyMatch(e -> e.length() > 6));
  }

  public void testCollect() {
    List<String> c = makeTestCollection();
    assertEquals(new HashSet<>(makeTestCollection()), c.collect(Collectors.toSet()));
  }

  public void testDistinct() {
    assertEquals(Arrays.asList('a', 'b', 'c').toSet(), Arrays.asList('a', 'b', 'b', 'c').distinct().toSet());
  }

  public void testFilter() {
    assertEquals(Arrays.asList("scott", "carson", "luca"), makeTestCollection().filter(e -> e.contains("c")).toList());
  }

  public void testGroupingBy() {
    Map<Boolean, List<String>> result = new HashMap<>();
    result.put(true, Arrays.asList("scott", "carson", "luca"));
    result.put(false, Arrays.asList("kyle"));
    assertEquals(result, makeTestCollection().groupingBy(e -> e.contains("c")));
  }

  public void testJoin() {
    assertEquals("scott; kyle; carson; luca", makeTestCollection().join("; "));
  }

  public void testMap() {
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), makeTestCollection().map(e -> e.charAt(0)).toList());
  }

  public void testMax() {
    assertEquals("scott", makeTestCollection().max(String::compareTo));
  }

  public void testMin() {
    assertEquals("carson", makeTestCollection().min(String::compareTo));
  }

  public void testNoneMatch() {
    assertTrue(makeTestCollection().noneMatch(e -> e.contains("z")));
    assertFalse(makeTestCollection().noneMatch(e -> e.contains("s")));
  }

  public void testReduce() {
    assertEquals("scottkylecarsonluca", makeTestCollection().reduce((e, f) -> e + f));
    String result = "foo";
    assertEquals("fooscottkylecarsonluca", makeTestCollection().reduce(result, (e, f) -> e + f));
  }

  public void testSorted() {
    assertEquals(Arrays.asList("carson", "kyle", "luca", "scott"), makeTestCollection().sorted().toList());
  }

  public void testToList() {
    assertEquals(makeTestCollection(), makeTestCollection().toList());
    assertEquals(makeTestCollection(), makeTestCollection().toSet().toList());
  }

  public void testToSet() {
    assertEquals(makeTestCollection(), makeTestCollection().toSet().toList());
  }

  public void testToMap() {
    Map<Character, String> result = new HashMap<>();
    result.put( 's', "scott" );
    result.put( 'k', "kyle" );
    result.put( 'c', "carson" );
    result.put( 'l', "luca" );
    assertEquals(result, makeTestCollection().toMap( e -> e.charAt(0)));

    Map<Character, Integer> resultV = new HashMap<>();
    resultV.put( 's', 5 );
    resultV.put( 'k', 4 );
    resultV.put( 'c', 6 );
    resultV.put( 'l', 4 );
    assertEquals(resultV, makeTestCollection().toMap( e -> e.charAt(0), e -> e.length() ));
  }

  private List<String> makeTestCollection() {
    return Arrays.asList("scott", "kyle", "carson", "luca");
  }

  private Iterable<String> makeTestIterable() {
    return Arrays.asList("fred", "barney");
  }

  private <T> List<T> lst(T... args) {
    return Arrays.asList(args);
  }
}
