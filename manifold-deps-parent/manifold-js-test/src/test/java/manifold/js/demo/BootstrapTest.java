package manifold.js.demo;

import demo.*;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class BootstrapTest {

  @Test
  public void primordialProgramTest() {
    assertEquals("Yay", BasicJavascriptProgram.bar());
    assertEquals(1.0, asDouble( BasicJavascriptProgram.incrementAndGet() ), 0 );
    assertEquals(2.0, asDouble( BasicJavascriptProgram.incrementAndGet() ), 0 );
    assertEquals("Foo", BasicJavascriptProgram.identity("Foo"));
    assertEquals(1, BasicJavascriptProgram.identity(1));
  }

  @Test
  public void primordialClassTest() {

    BasicJavascriptClass instance1 = new BasicJavascriptClass();
    assertEquals("Hello World", instance1.returnStr());
    assertEquals(1.0, asDouble( instance1.incrementAndGet() ), 0 );
    assertEquals(2.0, asDouble( instance1.incrementAndGet() ), 0 );
    assertEquals("Foo", instance1.identity("Foo"));
    assertEquals(1, instance1.identity(1));

    BasicJavascriptClass instance2 = new BasicJavascriptClass();
    assertEquals(1.0, asDouble( instance2.incrementAndGet() ), 0 );
    assertEquals(2.0, asDouble( instance2.incrementAndGet() ), 0 );

  }

  /**
   * needed for testing with Java 8 and Java 9 (nashorn changed behavior between those versions)
   */
  private double asDouble( Object n )
  {
    if( n instanceof Number )
    {
      return ((Number)n).doubleValue();
    }
    throw new RuntimeException( "Expecting Number, but found: " + n.getClass().getName() );
  }

}
