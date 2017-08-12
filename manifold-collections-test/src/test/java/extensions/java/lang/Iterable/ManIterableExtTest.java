package extensions.java.lang.Iterable;

import manifold.api.ExtensionManifoldTest;
import manifold.util.Pair;

import java.util.*;

public class ManIterableExtTest extends ExtensionManifoldTest {
  public void testCoverage() {
    testCoverage(ManIterableExt.class);
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

  public void testFilterIndexedToList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("carson"), iter.filterIndexedToList((i,e) -> i > 0 && e.contains("s")));
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

  public void testMapIndexedNotNullTo()  {
    Iterable<String> iter = makeTestIterableWithNulls();
    assertEquals(Arrays.asList(0, 1, 3, 4), iter.mapIndexedNotNullTo( new ArrayList<>(), (i,e) -> e == null ? null : i ));
  }

  public void testMapIndexedTo()  {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("0s", "1k", "2c", "3l"), iter.mapIndexedTo( new ArrayList<>(), (i,e) -> ""+i+ e.charAt( 0 ) ));
  }

  public void testMapNotNull()  {
    Iterable<String> iter = makeTestIterableWithNulls();
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), iter.mapNotNull( e -> e == null ? null : e.charAt(0) ));
  }

  public void testMapNotNullTo()  {
    Iterable<String> iter = makeTestIterableWithNulls();
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), iter.mapNotNullTo( new ArrayList<>(), e -> e == null ? null : e.charAt(0) ));
  }

  public void testMapTo() {
    Iterable<String> iter = makeTestIterableWithNulls();
    assertEquals(Arrays.asList('s', 'k', null, 'c', 'l'), iter.mapTo( new ArrayList<>(), e -> e == null ? null : e.charAt(0) ));
  }

  public void testMapToList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList('s', 'k', 'c', 'l'), iter.mapToList( e -> e == null ? null : e.charAt(0) ));
  }

  public void testMaxWith() {
    Iterable<String> iter = makeTestIterable();
    assertEquals("scott", iter.maxWith(String::compareTo));
  }

  public void testMinWith() {
    Iterable<String> iter = makeTestIterable();
    assertEquals("carson", iter.minWith(String::compareTo));
  }

  public void testPartition() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(new Pair<>(Arrays.asList("scott"), Arrays.asList("kyle", "carson", "luca")), iter.partition(e->e.compareTo("m") > 0));
  }

  public void testReversed() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("luca", "carson", "kyle", "scott"), iter.reversed());
  }

  public void testSingle() {
    Iterable<String> iter = Collections.singletonList("one");
    assertEquals("one", iter.single());

    iter = Arrays.asList("one", "two");
    try {
      iter.single();
      fail();
    }
    catch( IllegalArgumentException e )
    {
      // expected
    }

    iter = Collections.emptyList();
    try {
      iter.single();
      fail();
    }
    catch( NoSuchElementException e )
    {
      // expected
    }
  }

  public void testSingleOrNull() {
    Iterable<String> iter = Collections.singletonList("one");
    assertEquals("one", iter.singleOrNull());
    iter = Arrays.asList("one", "two");
    assertNull( iter.singleOrNull() );
    iter = Collections.emptyList();
    assertNull( iter.singleOrNull() );
  }

  public void testSubList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(Arrays.asList("kyle", "carson", "luca"), iter.subList( 1 ) );
    assertEquals(Arrays.asList("kyle", "carson"), iter.subList( 1, 3 ) );
  }

  public void testSubtract() {
    Iterable<String> iter = makeTestIterable();
    assertEquals(new HashSet<>(Arrays.asList("kyle", "luca")), iter.subtract( Arrays.asList("scott", "carson") ) );
  }

  public void testToList() {
    Iterable<String> iter = makeTestIterable();
    assertEquals( iter, iter.toList() );
    iter = makeNonCollection();
    assertEquals( makeTestIterable(), iter.toList() );
  }

  public void testToSet() {
    Iterable<String> iter = makeTestIterable();
    assertEquals( new HashSet<>(iter.toList()), iter.toSet() );
    iter = makeNonCollection();
    assertEquals( new HashSet<>(makeTestIterable().toList()), iter.toSet() );
  }

  public void testUnion() {
    Iterable<String> iter = makeTestIterable();
    List<String> other = Arrays.asList("a", "scott", "b", "kyle");
    HashSet<String> result = new HashSet<>( iter.toList() );
    result.addAll( other );
    assertEquals( result, iter.union(other) );
    iter = makeNonCollection();
    assertEquals( result, iter.union(other) );
  }

  private Iterable<String> empty() {
    return Collections.emptyList();
  }

  private Iterable<String> makeNonCollection() {
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return makeTestIterable().iterator();
      }
    };
  }

  public Iterable<String> makeTestIterable() {
    return Arrays.asList("scott", "kyle", "carson", "luca");
  }
  public Iterable<String> makeTestIterableWithNulls() {
    return Arrays.asList("scott", "kyle", null, "carson", "luca");
  }
}
