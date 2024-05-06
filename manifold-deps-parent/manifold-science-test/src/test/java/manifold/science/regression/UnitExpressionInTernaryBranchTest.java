package manifold.science.regression;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnitExpressionInTernaryBranchTest
{
  @Test
  public void testUnitExpressionInTernaryBranch()
  {
    ByteCoercion bt = ByteCoercion.INSTANCE;
    int i = 1;
    byte bb = i > 0 ? 2bt : (byte)1;
    assertEquals( 2bt, bb );
  }

  public static class ByteCoercion
  {
    static final ByteCoercion INSTANCE = new ByteCoercion();

    public byte postfixBind( int value )
    {
      return (byte)value;
    }
  }
}
