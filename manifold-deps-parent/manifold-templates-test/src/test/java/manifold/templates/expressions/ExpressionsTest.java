package manifold.templates.expressions;

import org.junit.Test;
import expressions.*;

import static org.junit.Assert.assertEquals;

public class ExpressionsTest
{
  @Test
  public void basicExpressionsWork()
  {
    assertEquals( "2", SimpleExpressionOrig.render() );
    assertEquals( "2", SimpleExpressionAlt.render() );
    assertEquals( "5", VariableExpressionOrig.render() );
    assertEquals( "5", VariableExpressionAlt.render() );
    assertEquals( "16", MethodCallExpressionOrig.render() );
    assertEquals( "16", MethodCallExpressionAlt.render() );
  }

}
