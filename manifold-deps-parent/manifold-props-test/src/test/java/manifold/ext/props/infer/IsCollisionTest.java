package manifold.ext.props.infer;

import org.junit.Test;

import static org.junit.Assert.*;

public class IsCollisionTest {
    @Test
    public void testIsCollision() {
        IsCollisionClass c = new IsCollisionClass("hi");
        assertSame( "hi", c.object );
        assertTrue(c.isObject);
        c.object = 5;
        assertFalse(c.isObject);
    }
}
