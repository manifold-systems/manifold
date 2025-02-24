package manifold.ext.props.infer;

import manifold.ext.props.rt.api.var;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AncestryTest
{
  @Test
  public void testInheritedProperty()
  {
    SubClass subClass = new SubClass();
    assertFalse(subClass.isEnabled);
    subClass.isEnabled = true;
    assertTrue(subClass.isEnabled);
  }

  static class SubClass extends BaseClass {}

  static class BaseClass
  {
    @var
    int hi;

    private boolean _enabled;
    public boolean isEnabled()
    {
      return _enabled;
    }
    public void setEnabled(boolean enabled) {
      _enabled = enabled;
    }

  }
}
