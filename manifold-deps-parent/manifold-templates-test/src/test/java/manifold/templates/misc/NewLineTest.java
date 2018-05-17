package manifold.templates.misc;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class NewLineTest
{
  @Test
  public void testForNewLines()
  {
    assertEquals( "This is a ManTL (Manifold Template Language) file.\n" +
                  "\n" +
                  "You can render this template type-safely from Java like this:\n" +
                  "\n" +
                  "TemplateName.render(\"Hello World!\");\n" +
                  "\n" +
                  "You can declare any number of parameters in the 'params' directive, import\n" +
                  "types and packages using the 'import' directive, extend a special template\n" +
                  "class using the 'extends' directive, include content from other templates,\n" +
                  "define sections, and many other useful features.\n" +
                  "\n" +
                  "You can use Java statements:\n" +
                  "\n" +
                  "The value of 'param1' is: Hello World!!!\n" +
                  "\n" +
                  "And you can use Java expressions:\n" +
                  "\n" +
                  "H\n" +
                  "H\n" +
                  "\n" +
                  "Learn more: http://manifold.systems/manifold-templates.html",
      misc.TestNewLines.render( "Hello World!!!" ) );
  }
}