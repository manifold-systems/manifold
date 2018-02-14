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
    Assert.assertEquals( "    This template loops through and prints all the gosu team names\n" +
                         "    Cool Guys:\n" +
                         "        -Carson\n" +
                         "        -Scott\n" +
                         "        -Kyle\n" +
                         "        -Luca\n" +
                         "        done",
      JavascriptTemplate.renderToString( Arrays.asList( "Carson", "Scott", "Kyle", "Luca" ) ) );
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

