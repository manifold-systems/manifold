package manifold.templates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BootstrapTest {

    @Test
    public void bootstrap() {
        assertEquals("<html>\n" +
                "<body>\n" +
                "<h1>Hello BB Templates!</h1>\n" +
                "</body>\n" +
                "</html>", demo.Sample1.render());
    }

}
