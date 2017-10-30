package manifold.templates.directives;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import directives.extend.*;

/**
 * Created by hkalidhindi on 7/20/2017.
 */
public class ExtendsTest {

    @Test
    public void extendsWorks() {
        assertEquals("1234", simpleExtends.render());
    }

}
