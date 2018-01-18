package manifold.templates.misc;

import org.junit.Test;
import misc.*;

import static org.junit.Assert.assertEquals;

public class EmptyFileTest {

    @Test
    public void emptyFileWorks() {
        assertEquals("", EmptyTest.render());
    }

}