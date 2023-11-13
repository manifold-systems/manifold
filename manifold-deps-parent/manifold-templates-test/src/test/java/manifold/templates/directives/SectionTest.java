package manifold.templates.directives;

import org.junit.Test;
import directives.section.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SectionTest
{
  @Test
  public void basicSectionWorks()
  {
    assertEquals( "This is a section test (this is a section)", SimpleSection.render() );
  }

  @Test
  public void nestedSectionCallWorks()
  {
    assertEquals( "(this is a section)", SimpleSection.mySection.render() );
    assertEquals( "(Edward is 19 years old)", SectionWithParams.mySection.render( "Edward", 19 ) );
  }

  @Test
  public void sectionWithParamsWorks()
  {
    assertEquals( "Section with Params (Carson is 9001 years old)", SectionWithParams.render() );
  }

  @Test
  public void simpleInferenceWorks()
  {
    assertEquals( "Inference Test (Carson is 9001 years old)", SimpleInference.render() );
  }

  @Test
  public void inferenceInParamsWorks()
  {
    assertEquals( "Infer in Params Test (Carson is 9001 years old)", InferenceInParams.render( "Carson", 9001 ) );
  }

  @Test
  public void inferenceInCodeBlockWorks()
  {
    assertEquals( "Inference in Code Block (0)(1)(2)(3)(4)", InferenceInCodeBlock.render() );
  }

  @Test
  public void doubleInferenceWorks()
  {
    assertEquals( "First Section: 10Second Section: 10", SectionWithinSection.render( 10 ) );
  }

  @Test
  public void includeNestedSectionTest()
  {
    String result =
      "begin\n" +
      "        <h2 style=\"font-size: 1\">Font size: 1</h2>\n" +
      "        <h2 style=\"font-size: 2\">Font size: 2</h2>\n" +
      "        <h2 style=\"font-size: 3\">Font size: 3</h2>\n" +
      "end";
    String output = demo.IncludeNestedSectionTest.render();
    assertTrue(output.contains("        <h2 style=\"font-size: 1\">Font size: 1</h2>\n") &&
            output.contains("        <h2 style=\"font-size: 2\">Font size: 2</h2>\n") &&
            output.contains("        <h2 style=\"font-size: 3\">Font size: 3</h2>\n"));
  }

}