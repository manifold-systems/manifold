package manifold.ext.props;

import junit.framework.TestCase;
import manifold.ext.props.rt.api.override;
import manifold.ext.props.rt.api.var;

public class DoubleImplementsTest extends TestCase
{
  public void testDoubleImplements()
  {
    MyClass2 myClass2 = new MyClass2();
    Object object = myClass2.object;
    assertNull(object);
    myClass2.object = "hello";
    assertEquals(myClass2.object, "hello");
    MyClass myClass = myClass2;
    myClass.object = "hi";
    assertEquals(myClass.object, "hi");
  }

  public interface TestIntf {
    Object getObject();
  }

  public static class MyClass implements TestIntf {
    @override @var Object object;

    void whatever()
    {
      object = "hey";
    }
  }

  public static class MyClass2 extends MyClass implements TestIntf {
    public void foo(){
      object = "hi"; // should not be ambiguous ref
      Object value = object; // should not be ambiguous ref

      MyClass2 mc2 = new MyClass2();
      mc2.object = value;
    }
  }
}
