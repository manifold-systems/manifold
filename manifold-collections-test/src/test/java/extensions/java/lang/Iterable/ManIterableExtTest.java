package extensions.java.lang.Iterable;

import junit.framework.TestCase;
import manifold.ext.api.This;
import manifold.util.IndexedPredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ManIterableExtTest extends TestCase {
    public void testCollectionIterable() {
        Iterable<String> iter = Arrays.asList( "scott", "kyle", "carson", "luca" );

        assertEquals( 4, iter.count() );
        assertEquals( 3, iter.count( e -> e.contains( "c" ) ) );

        List<String> dest = new ArrayList<>();
        List<String> result = iter.filterIndexedTo( dest, (i, e) -> i > 0 && e.contains( "c" ) );
        assertSame( dest, result );
        assertEquals( Arrays.asList( "carson", "luca" ), result );
    }
}
