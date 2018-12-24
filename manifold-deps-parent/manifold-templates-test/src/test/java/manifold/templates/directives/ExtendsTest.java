package manifold.templates.directives;

import org.junit.Test;
import directives.extend.*;

import static org.junit.Assert.assertEquals;

public class ExtendsTest
{
  @Test
  public void extendsWorks()
  {
    assertEquals( "1234", simpleExtends.render() );
  }
}
