package manifold.ext.props.infer;

import manifold.ext.props.middle.auto.Square;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayTest {
    @Test
    public void testArrayProperty()
    {
        Square sq = new Square( 4 );
        assertEquals( "a", sq.stringArray[0] );
        assertEquals( "abc", sq.stringArray[0] += "bc" );
    }
}
