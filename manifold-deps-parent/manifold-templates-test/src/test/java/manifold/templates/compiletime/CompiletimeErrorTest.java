package manifold.templates.compiletime;

import java.util.ArrayList;
import java.util.List;
import manifold.templates.codegen.TemplateGen;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompiletimeErrorTest
{
  @Test
  public void ExtendsErrorTest()
  {
    TemplateGen generator = new TemplateGen();
    generator.generateCode( "testing.tester",
      "<%@ extends ExtendsTesterTemplate %><%@ extends ExtendsTesterTemplate %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section mySection %><%@ extends ExtendsTesterTemplate %><%@ extends ExtendsTesterTemplate %><%@ end section %>", null,
      "tester.manifold.templates.html" );

    List<String> expectedMessages = new ArrayList<>();
    expectedMessages.add( "Invalid Extends Directive: class cannot extend 2 classes" );
    expectedMessages.add( "Invalid Extends Directive: class cannot extend within section" );
    expectedMessages.add( "Invalid Extends Directive: class cannot extend within section" );

    assertEquals( expectedMessages.size(), generator.getIssues().getIssues().size() );
    for( int i = 0; i < expectedMessages.size(); i += 1 )
    {
      assertEquals( generator.getIssues().getIssues().get( i ).getMessage(), expectedMessages.get( i ) );
    }

  }

  @Test
  public void ParamsErrorTest()
  {
    TemplateGen generator = new TemplateGen();
    generator.generateCode( "testing.tester",
      "<%@ params(String name, int age) %><%@ params(String name, int age) %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section mySection %><%@ params(String name, int age) %><%@ end section %>", null,
      "tester.manifold.templates.html" );

    List<String> expectedMessages = new ArrayList<>();
    expectedMessages.add( "Invalid Params Directive: class cannot have 2 params directives" );
    expectedMessages.add( "Invalid Params Directive: class cannot have param directive within section" );

    assertEquals( expectedMessages.size(), generator.getIssues().getIssues().size() );
    for( int i = 0; i < expectedMessages.size(); i += 1 )
    {
      assertEquals( generator.getIssues().getIssues().get( i ).getMessage(), expectedMessages.get( i ) );
    }

  }

  @Test
  public void SectionErrorTest()
  {
    TemplateGen generator = new TemplateGen();
    generator.generateCode( "testing.tester",
      "<%@ section mySection %><%@ extends ExtendsTesterTemplate %><%@ end section %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section mySection %><%@ params(String name, int age) %><%@ end section %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ end section %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section MySection %>", null,
      "tester.manifold.templates.html" );

    List<String> expectedMessages = new ArrayList<>();
    expectedMessages.add( "Invalid Extends Directive: class cannot extend within section" );
    expectedMessages.add( "Invalid Params Directive: class cannot have param directive within section" );
    expectedMessages.add( "Invalid End Section Directive: section declaration does not exist" );
    expectedMessages.add( "Reached end of file before parsing section: MySection" );


    assertEquals( expectedMessages.size(), generator.getIssues().getIssues().size() );
    for( int i = 0; i < expectedMessages.size(); i += 1 )
    {
      assertEquals( generator.getIssues().getIssues().get( i ).getMessage(), expectedMessages.get( i ) );
    }

  }

  @Test
  public void IsLayoutErrorTest()
  {
    TemplateGen generator = new TemplateGen();
    generator.generateCode( "testing.tester",
      "<%@ content %><%@ content %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section mySection %><%@ content %><%@ end section %>", null,
      "tester.manifold.templates.html" );

    List<String> expectedMessages = new ArrayList<>();
    expectedMessages.add( "Invalid Layout Instantiation: cannot have two layout instantiations" );
    expectedMessages.add( "Invalid Layout Instantiation: cannot instantiate layout within section" );

    assertEquals( expectedMessages.size(), generator.getIssues().getIssues().size() );
    for( int i = 0; i < expectedMessages.size(); i += 1 )
    {
      assertEquals( generator.getIssues().getIssues().get( i ).getMessage(), expectedMessages.get( i ) );
    }

  }

  @Test
  public void HasLayoutErrorTest()
  {
    TemplateGen generator = new TemplateGen();
    generator.generateCode( "testing.tester",
      "<%@ layout directives.layouts.IsLayout%><%@ layout directives.layouts.IsLayout%>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section mySection %><%@ layout directives.layouts.IsLayout %><%@ end section %>", null,
      "tester.manifold.templates.html" );

    List<String> expectedMessages = new ArrayList<>();
    expectedMessages.add( "Invalid Layout Declaration: cannot have two layout declarations" );
    expectedMessages.add( "Invalid Layout Declaration: cannot declare layout within section" );

    assertEquals( expectedMessages.size(), generator.getIssues().getIssues().size() );
    for( int i = 0; i < expectedMessages.size(); i += 1 )
    {
      assertEquals( generator.getIssues().getIssues().get( i ).getMessage(), expectedMessages.get( i ) );
    }

  }

  @Test
  public void UnsupportedTypeErrorTest()
  {
    TemplateGen generator = new TemplateGen();
    generator.generateCode( "testing.tester",
      "<%@ invalidDirective %>", null,
      "tester.manifold.templates.html" );
    generator.generateCode( "testing.tester",
      "<%@ section MySection(apple) %> <%@ end section %>", null,
      "tester.manifold.templates.html" );

    List<String> expectedMessages = new ArrayList<>();
    expectedMessages.add( "Unsupported Directive Type" );
    expectedMessages.add( "Type for argument can not be inferred: apple" );

    assertEquals( expectedMessages.size(), generator.getIssues().getIssues().size() );
    for( int i = 0; i < expectedMessages.size(); i += 1 )
    {
      assertEquals( generator.getIssues().getIssues().get( i ).getMessage(), expectedMessages.get( i ) );
    }

  }

}
