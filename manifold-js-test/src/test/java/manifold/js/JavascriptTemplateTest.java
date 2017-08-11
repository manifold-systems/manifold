package manifold.js;

import demo.*;
import org.junit.Test;

import java.util.Arrays;

public class JavascriptTemplateTest {

  @Test
  public void testBasicTemplates() {
    System.out.println(demo.JavascriptTemplate.renderToString(Arrays.asList("Carson", "Scott", "Kyle", "Luca")));
  }

}

