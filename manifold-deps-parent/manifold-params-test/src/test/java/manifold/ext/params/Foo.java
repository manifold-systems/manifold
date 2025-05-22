package manifold.ext.params;

public class Foo
{
  public String optionalParamsNameAge(String name, int age =100 )
  {
    return name + "," + age;
  }
}
