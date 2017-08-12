package extensions.java.lang.Iterable;

import manifold.api.ExtensionManifoldTest;

import java.util.*;

public class ManIterableExtTest extends ExtensionManifoldTest {
  public void testCoverage() {
    /*testCoverage(ManIterableExt.class)*/
    ;
  }

  public void testCount() {
    Iterable<String> iter = makeTestIterable();

    assertEquals(4, iter.count());
    assertEquals(3, iter.count(e -> e.contains("c")));
  }

  public void testFilterIndexedTo() {
    Iterable<String> iter = makeTestIterable();

    List<String> dest = new ArrayList<>();
    List<String> result = iter.filterIndexedTo(dest, (i, e) -> i > 0 && e.contains("c"));
    assertSame(dest, result);
    assertEquals(Arrays.asList("carson", "luca"), result);
  }

  public void testDistinctBy() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(3, iter.distinctBy(e -> e.length()).size());
  }

  public void testFilteredIndexToList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("scott", "carson"), iter.filterIndexedToList((i, e) -> i % 2 == 0));
  }

  public void testDistinctList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(makeTestIterable().toList(), iter.distinctList());
  }

  public void testFilterNotToList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("kyle", "carson", "luca"), iter.filterNotToList(e -> e.contains("scott")));
  }

  public void testFilterNotTo() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("scott", "carson"), iter.filterNotTo(new ArrayList<>(), e -> e.contains("l")));
  }

  public void testFilterTo() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("kyle", "luca"), iter.filterTo(new ArrayList<>(), e -> e.contains("l")));
  }

  public void testFilterToList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("kyle", "luca"), iter.filterToList(e -> e.contains("l")));
  }

  public void testFirst() {
    Iterable<String> iter = makeTestIterable();
    assertEquals("scott", iter.first());
    assertEquals("kyle", iter.first(e -> e.contains("l")));
  }

  public void testFirstOrNull() {
    Iterable<String> iter = makeTestIterable();
    assertEquals("scott", iter.firstOrNull());
    assertNull(empty().firstOrNull());
    assertEquals("kyle", iter.firstOrNull(e -> e.contains("l")));
    assertNull(iter.firstOrNull(e -> e.contains("z")));
  }

  public void testJoinToString() {
    Iterable<String> iter = makeTestIterable();
    assertEquals("scott; kyle; carson; luca", iter.joinToString("; "));
  }

  public void testJoinTo() {
    Iterable<String> iter = makeTestIterable();
    StringBuilder sb = new StringBuilder();
    assertSame(sb, iter.joinTo(sb, "; "));
    assertEquals("scott; kyle; carson; luca", iter.joinTo(new StringBuilder(), "; ").toString());
  }

  public void testFlatMap() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), iter.flatMap(s -> Arrays.asList(s.charAt(0))));
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), iter.flatMapTo(new ArrayList<>(), s -> Arrays.asList(s.charAt(0))));
  }

  public void testFlatMapTo() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), iter.flatMapTo(new ArrayList<>(), s -> Arrays.asList(s.charAt(0))));
  }

  public void testFold() {
    Iterable<String> iter = makeTestIterable();
    assertEquals("skcl", iter.fold("", (a, s) -> a + s.charAt(0)));
  }

  public void testForEachIndexed() {
    Iterable<String> iter = makeTestIterable();
    StringBuilder sb = new StringBuilder();
    iter.forEachIndexed( (i,e) -> sb.append( i ).append( e.charAt( 0 ) ) );
    assertEquals("0s1k2c3l", sb.toString() );
  }

  public void testIndexOfFirst() {
    Iterable<String> iter = makeTestIterable();
    assertEquals( 1, iter.indexOfFirst( e -> e.contains("l") ) );
  }

  public void testIndexOfLast() {
    Iterable<String> iter = makeTestIterable();
    assertEquals( 3, iter.indexOfLast( e -> e.contains("l") ) );
  }

  public void testIntersect() {
    Iterable<String> iter = makeTestIterable();
    assertEquals( new HashSet<>( Arrays.asList( "kyle", "luca") ), iter.intersect( Arrays.asList( "kyle", "barney", "luca", "fred" ) ) );
  }

  public void testLast()  {
    Iterable<String> iter = makeTestIterable();
    assertEquals("luca", iter.last());
    assertEquals("carson", iter.last(e -> e.contains("s")));
  }

  public void testLastOrNull()  {
    Iterable<String> iter = makeTestIterable();
    assertEquals("luca", iter.lastOrNull());
    assertNull( empty().lastOrNull());
    assertEquals("carson", iter.lastOrNull(e -> e.contains("s")));
    assertNull("carson", iter.lastOrNull(e -> e.contains("z")));
  }

  public void testMapIndexed()  {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("0s", "1k", "2c", "3l"), iter.mapIndexed( (i,e) -> ""+i+ e.charAt( 0 ) ));
  }

  public void testMapIndexedNotNull()  {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("0s", "1k", "2c", "3l"), iter.mapIndexed( (i,e) -> ""+i+ e.charAt( 0 ) ));
  }

  private Iterable<String> empty() {
    return Collections.emptyList();
  }

  public Iterable<String> makeTestIterable() {
    return Arrays.asList("scott", "kyle", "carson", "luca");
  }
}
