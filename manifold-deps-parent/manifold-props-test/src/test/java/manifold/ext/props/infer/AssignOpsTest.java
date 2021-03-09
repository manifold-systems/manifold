package manifold.ext.props.infer;

import manifold.ext.props.middle.auto.Shape;
import manifold.ext.props.middle.auto.Square;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssignOpsTest {
    @Test
    public void testAssignOp_FieldAccess() {
        Square sq = new Square(4);

        int[] callCount = {0};
        double scale = self(sq, callCount).scale += 3;
        assertEquals(4, scale, 0);
        assertEquals(4, sq.scale, 0);
        assertEquals(1, callCount[0]);

        int[] callCount2 = {0};
        self(sq, callCount2).scale += 3;
        assertEquals(7, sq.scale, 0);
        assertEquals(1, callCount[0]);
    }

    @Test
    public void testUnaryPostInc_simple() {
        Square sq = new Square(4);

        double scale = sq.scale++;
        assertEquals(1, scale, 0);
        assertEquals(2, sq.scale, 0);

        sq.scale++;
        assertEquals(3, sq.scale, 0);
    }

    @Test
    public void testUnaryPreInc_simple() {
        Square sq = new Square(4);

        double scale = ++sq.scale;
        assertEquals(2, scale, 0);
        assertEquals(2, sq.scale, 0);

        ++sq.scale;
        assertEquals(3, sq.scale, 0);
    }

    @Test
    public void testUnaryPostInc_complex() {
        Square sq = new Square(4);

        int[] callCount = {0};
        double scale = self(sq, callCount).scale++;
        assertEquals(1, scale, 0);
        assertEquals(2, sq.scale, 0);
        assertEquals(1, callCount[0]);

        int[] callCount2 = {0};
        self(sq, callCount2).scale++;
        assertEquals(3, sq.scale, 0);
        assertEquals(1, callCount2[0]);
    }

    @Test
    public void testUnaryPreInc_complex() {
        Square sq = new Square(4);

        int[] callCount = {0};
        double scale = ++self(sq, callCount).scale;
        assertEquals(2, scale, 0);
        assertEquals(2, sq.scale, 0);
        assertEquals(1, callCount[0]);

        int[] callCount2 = {0};
        ++self(sq, callCount2).scale;
        assertEquals(3, sq.scale, 0);
        assertEquals(1, callCount2[0]);
    }

    @Test
    public void testAssignOp_Ident() {
        Square sq = new Square(4);
        sq.testAssignOp_Ident(3);
        assertEquals(4, sq.scale, 0);
    }

    private <T extends Shape> T self(T shape, int[] callCount) {
        callCount[0]++;
        return shape;
    }
}
