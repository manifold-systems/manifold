package manifold.templates.directives;

import org.junit.Test;
import directives.imports.*;

import static org.junit.Assert.assertEquals;

public class ImportTest
{
  @Test
  public void basicImportsWork()
  {
    assertEquals( "1", SimpleImport.render() );
  }

  @Test
  public void multipleImportsWork()
  {
    assertEquals( "1hello", MultipleImport.render() );
  }
}
