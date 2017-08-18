package manifold.js.demo;

import demo.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArrowFunctionTest {

  @Test
  public void testBasicArrowFunctionsWork() {
    Object result = ArrowFunctionClass.arrowFilterStatement();
    assertEquals(3, result);
  }

}
