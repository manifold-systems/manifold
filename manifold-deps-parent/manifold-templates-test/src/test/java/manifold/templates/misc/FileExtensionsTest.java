package manifold.templates.misc;

import org.junit.Test;
import misc.*;
import static org.junit.Assert.assertEquals;

public class FileExtensionsTest {

    @Test
    public void variousExtensionsWork() {
        assertEquals("mtf", ExtensionTest1.render());
        assertEquals("html", ExtensionTest2.render());
        assertEquals("sql", ExtensionTest3.render());
    }

}