package extensions.java.util.stream.Stream;

import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ManifoldStreamExtTest {

    @Test
    public void embellishments(){

        // embellishments
        Stream<String> stream = stream("a", "aa", "aaa");
        assertEquals(stream, stream.toList());
        assertEquals(new HashSet<>(stream.toList()), stream.toSet());
        assertEquals(new TreeSet<>(stream.toList()), stream.toSortedSet());

        Map<Integer, String> byLen = stream.toMap(String::length);
        assertEquals("a", byLen.get(1));
        assertEquals("aaa", byLen.get(3));

        Map<Integer, Integer> hashCodeByLen = stream.toMap(String::length, String::hashCode);
        assertEquals((Integer) "a".hashCode(), hashCodeByLen.get(1));
        assertEquals((Integer) "aaa".hashCode(), hashCodeByLen.get(3));

        Map<Integer, List<String>> stringsByLength = stream("a", "b", "aa", "ab", "abc").groupingBy(String::length);
        assertEquals(2, stringsByLength.get(1).size());
        assertEquals(2, stringsByLength.get(2).size());
        assertEquals(1, stringsByLength.get(3).size());
        assertEquals(null, stringsByLength.get(4));
    }

    private <T> Stream<T> stream(T... args) {
        return Arrays.asList(args).stream();
    }
}
