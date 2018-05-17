package manifold.templates.runtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface ILayout {

    public static final ILayout EMPTY = new ILayout() {
        @Override
        public void header(Appendable buffer) throws IOException { return; }

        @Override
        public void footer(Appendable buffer) throws IOException { return; }
    };

    void header(Appendable buffer) throws IOException;

    void footer(Appendable buffer) throws IOException;

}
