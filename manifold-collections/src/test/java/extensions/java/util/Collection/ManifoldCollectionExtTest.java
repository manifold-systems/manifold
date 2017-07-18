package extensions.java.util.Collection;

import org.junit.Test;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ManifoldCollectionExtTest {

    @Test
    public void passthroughs(){

        // straight pass through to stream tests
        assertEquals(lst(1, 2, 3), lst("a", "aa", "aaa").map(String::length).toList());
        assertEquals(lst("aa", "aaa"), lst("a", "aa", "aaa").filter(s -> s.length() > 1).toList());
        assertEquals("aaaaaa", lst("a", "aa", "aaa").collect(Collectors.joining()));

        assertEquals(lst("a", "b", "c"), lst("b", "c", "a").sorted().toList());
        assertEquals(lst("c", "b", "a"), lst("b", "c", "a").sorted(Comparator.reverseOrder()).toList());
        assertEquals("bca", lst("b", "c", "a").reduce("", String::concat));

        assertEquals(true, lst("b", "c", "a").anyMatch(s -> s.equals("a")));
        assertEquals(true, lst("b", "c", "a").allMatch(s -> s.length() > 0));
        assertEquals(true, lst("b", "c", "a").noneMatch(s -> s.length() == 0));

    }

    @Test
    public void removeOptional(){

        // remove optional tests
        assertEquals("bca", lst("b", "c", "a").reduce(String::concat));
        assertEquals("c", lst("b", "c", "a").max(String::compareTo));
        assertEquals("a", lst("b", "c", "a").min(String::compareTo));
    }

    @Test
    public void embellishments(){

        // embellishments
        List<String> lst = lst("a", "aa", "aaa");
        assertEquals("a,aa,aaa", lst.join(","));
        assertEquals(lst, lst.toList());
        assertEquals(new HashSet<>(lst), lst.toSet());
        assertEquals(new TreeSet<>(lst), lst.toSortedSet());

        Map<Integer, String> byLen = lst.toMap(String::length);
        assertEquals("a", byLen.get(1));
        assertEquals("aaa", byLen.get(3));

        Map<Integer, Integer> hashCodeByLen = lst.toMap(String::length, String::hashCode);
        assertEquals((Integer) "a".hashCode(), hashCodeByLen.get(1));
        assertEquals((Integer) "aaa".hashCode(), hashCodeByLen.get(3));

        Map<Integer, List<String>> stringsByLength = lst("a", "b", "aa", "ab", "abc").groupingBy(String::length);
        assertEquals(2, stringsByLength.get(1).size());
        assertEquals(2, stringsByLength.get(2).size());
        assertEquals(1, stringsByLength.get(3).size());
        assertEquals(null, stringsByLength.get(4));

    }

    private <T> List<T> lst(T... args) {
        return Arrays.asList(args);
    }
}
