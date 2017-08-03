package manifoldjs;

import demo.*;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class BootstrapTest {

  @Test
  public void primordialProgramTest() {
    assertEquals("Yay", BasicJavascriptProgram.bar());
    assertEquals(1.0, BasicJavascriptProgram.incrementAndGet());
    assertEquals(2.0, BasicJavascriptProgram.incrementAndGet());
    assertEquals("Foo", BasicJavascriptProgram.identity("Foo"));
    assertEquals(1, BasicJavascriptProgram.identity(1));
  }

  @Test
  public void primordialClassTest() {

    BasicJavascriptClass instance1 = new BasicJavascriptClass();
    assertEquals("Hello World", instance1.returnStr());
    assertEquals(1.0, instance1.incrementAndGet());
    assertEquals(2.0, instance1.incrementAndGet());
    assertEquals("Foo", instance1.identity("Foo"));
    assertEquals(1, instance1.identity(1));

    BasicJavascriptClass instance2 = new BasicJavascriptClass();
    assertEquals(1.0, instance2.incrementAndGet());
    assertEquals(2.0, instance2.incrementAndGet());

  }

}
