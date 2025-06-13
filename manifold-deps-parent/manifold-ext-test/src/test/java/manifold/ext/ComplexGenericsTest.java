package manifold.ext;

import abc.ComplexGenerics;
import junit.framework.TestCase;

public class ComplexGenericsTest extends TestCase 
{
  public void testComplexGenerics()
  {
    ComplexGenerics complexGenerics = new ComplexGenerics();
    complexGenerics.addWidget( null );
    complexGenerics.helloWorld();
  }
}
