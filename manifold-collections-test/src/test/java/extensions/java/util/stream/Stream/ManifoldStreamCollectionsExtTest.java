package extensions.java.util.stream.Stream;

import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ManifoldStreamCollectionsExtTest
{

    @Test
    public void embellishments(){

        // embellishments
        List<String> sampleList = Arrays.asList("a", "aa", "aaa");

        assertEquals( sampleList, stream(sampleList).toList());

        assertEquals( new HashSet<>(stream(sampleList).toList()), stream(sampleList).toSet());

        Map<Integer, String> byLen = stream(sampleList).toMap( String::length);
        assertEquals("a", byLen.get(1));
        assertEquals("aaa", byLen.get(3));

        Map<Integer, Integer> hashCodeByLen = stream(sampleList).toMap( String::length, String::hashCode);
        assertEquals((Integer) "a".hashCode(), hashCodeByLen.get(1));
        assertEquals((Integer) "aaa".hashCode(), hashCodeByLen.get(3));

        Map<Integer, List<String>> stringsByLength = stream(Arrays.asList("a", "b", "aa", "ab", "abc")).groupingBy(String::length);
        assertEquals(2, stringsByLength.get(1).size());
        assertEquals(2, stringsByLength.get(2).size());
        assertEquals(1, stringsByLength.get(3).size());
        assertEquals(null, stringsByLength.get(4));
    }

    private <T> Stream<T> stream(Collection<T> c) {
        return c.stream();
    }
}
