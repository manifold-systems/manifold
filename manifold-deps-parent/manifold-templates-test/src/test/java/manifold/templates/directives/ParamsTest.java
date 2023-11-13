package manifold.templates.directives;

import java.util.HashSet;
import java.util.LinkedHashSet;
import org.junit.Test;
import directives.params.*;

import static org.junit.Assert.assertEquals;

public class ParamsTest
{
  @Test
  public void simpleParamsWorks()
  {
    assertEquals( "Edward", SimpleParams.render( "Edward" ) );
  }

  @Test
  public void multipleParamsWorks()
  {
    assertEquals( "Name:EdwardAge:19", MultipleParams.render( "Edward", 19 ) );
  }

  @Test
  public void importedParamWorks()
  {
    HashSet<Integer> ages = new LinkedHashSet<>();
    ages.add( 1 );
    ages.add( 2 );
    ages.add( 3 );
    assertEquals( "123", ImportedParams.render( ages ) );
  }
}