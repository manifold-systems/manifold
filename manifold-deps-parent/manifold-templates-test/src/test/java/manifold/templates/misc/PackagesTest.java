package manifold.templates.misc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PackagesTest {

    @Test
    public void variousExtensionsWork() {
        assertEquals("package1", misc.package1.Package1Template.render());
        assertEquals("package2", misc.package1.package2.Package2Template.render());
    }

}