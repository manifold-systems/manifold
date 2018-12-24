package manifold.js.demo;

import demo.JavascriptTemplate;
import demo.JavascriptTemplateNoParams;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class JavascriptTemplateTest
{
  @Test
  public void testBasicTemplates()
  {
    Assert.assertEquals( "    This template loops through and prints stuff\n" +
                         "    Stuff:\n" +
                         "        -abc\n" +
                         "        -def\n" +
                         "        -ghi\n" +
                         "        -jkl\n" +
                         "        done",
      JavascriptTemplate.renderToString( Arrays.asList( "abc", "def", "ghi", "jkl" ) ) );
  }

  @Test
  public void testNoParamTemplates()
  {
    Assert.assertEquals( "Some text\n" +
                         "5\n" +
                         "some more text",
      JavascriptTemplateNoParams.renderToString() );
  }

}

