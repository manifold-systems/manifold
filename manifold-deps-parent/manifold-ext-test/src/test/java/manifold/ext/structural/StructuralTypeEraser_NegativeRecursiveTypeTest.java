package manifold.ext.structural;

public class StructuralTypeEraser_NegativeRecursiveTypeTest {
    public void testNothing() {
        // Compiling this class wo error is the test, the lambda in AbstractSetter:
        //
        //     next -> this.visit0(next)
        //
        // Tests that the recursive functional interface `InternalChildCallback` is _not_ erased during the structural
        // interface check performed by StructuralTypeEraser.
    }

    @FunctionalInterface
    interface InternalChildCallback<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
        int visitChild(Callback<M, C> next);
    }

    interface Callback<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
    }

    static class AbstractSetter {
        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visitChildFixed(InternalChildCallback<M, C> childCallback, Callback<M, C> callback) {
            return 0;
        }
    }

    static class BasicSetterImpl extends AbstractSetter {
        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visit0(Callback<M, C> callback) {
            return this.visitChildFixed(next -> this.visit0(next), callback);
        }
    }

    static abstract class StructLayout<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
        public interface Member<M extends Member<M, C>, C extends Component> {
        }

        public interface Component {
        }
    }
}
