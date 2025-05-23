package manifold.ext.params;


import junit.framework.TestCase;
import manifold.ext.params.middle.MyTestClass;
import manifold.ext.params.middle.MyTestSubClass;

/**
 * Test using methods having optional parameters in compiled classes, as opposed to methods from source in the same module
 */
public class ExternalTest extends TestCase
{
  public void testExternalOptionalParamsMethod()
  {
    MyTestClass mtc = new MyTestClass();
    assertEquals( "scott:100", mtc.myMethod( "scott" ) );
    assertEquals( "scott:99", mtc.myMethod( "scott", age:99 ) );
  }

  public void testExternalOptionalParamsMethodSubclass()
  {
    MyTestSubClass sub = new MyTestSubClass();
    assertEquals( "scott:200:false", sub.myMethod( "scott" ) );
    assertEquals( "scott:98:false", sub.myMethod( "scott", age:98 ) );
    assertEquals( "scott:200:true", sub.myMethod( "scott", extra:true ) );
    assertEquals( "scott:97:true", sub.myMethod( "scott", age:97, extra:true ) );
  }

  public void testExternalOptionalParamsMethodOverride()
  {
    MyTestClass mtc = new MyTestSubClass();
    assertEquals( "scott:200:false", mtc.myMethod( "scott" ) );
    assertEquals( "scott:98:false", mtc.myMethod( "scott", age:98 ) );
  }

  public void testExternalOptionalParamsMethodOverrideLocal()
  {
    MyTestClass mtc = new MyLocalTestSubClass();
    assertEquals( "scott:200:false", mtc.myMethod( "scott" ) );
    assertEquals( "scott:98:false", mtc.myMethod( "scott", age:98 ) );
  }

  static class MyLocalTestSubClass extends MyTestClass
  {
    @Override
    public String myMethod( String name, int age = 200, boolean extra = false )
    {
      return name + ":" + age + ":" + extra;
    }
  }
}
