package manifold.ext.params.middle;

public class MyTestSubClass extends MyTestClass
{
  @Override
  public String myMethod( String name, int age = 200, boolean extra = false )
  {
    return name + ":" + age + ":" + extra;
  }
}
