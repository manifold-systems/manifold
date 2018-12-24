package manifold.templates.directives;

import org.junit.Test;
import directives.include.*;

import static org.junit.Assert.assertEquals;

public class IncludeTest
{
  @Test
  public void basicIncludeWorks()
  {
    assertEquals( "15", SimpleInclude.render() );
  }

  @Test
  public void includeWithParamsWorks()
  {
    assertEquals( "Carson", IncludeWithParams.render() );
  }

  @Test
  public void includeWithMultipleParamsWorks()
  {
    assertEquals( "Name:CarsonAge:2000", IncludeWithMultipleParams.render() );
  }

  @Test
  public void conditionalIncludeWithParamsWorks()
  {
    assertEquals( "Carson", ConditionalIncludeWithParams.render( true ) );
    assertEquals( "", ConditionalIncludeWithParams.render( false ) );
  }

  @Test
  public void conditionalIncludeWithoutParamsWorks()
  {
    assertEquals( "15", SimpleConditionalInclude.render( true ) );
    assertEquals( "", SimpleConditionalInclude.render( false ) );
  }
}
