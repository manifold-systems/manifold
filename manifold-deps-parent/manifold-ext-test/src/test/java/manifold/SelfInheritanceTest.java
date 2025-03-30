package manifold;

import manifold.ext.rt.api.Self;
import org.junit.Test;

public class SelfInheritanceTest
{
  public interface MyIntf {
    @Self MyIntf doSomethingAndReturnYourself();
  }

  public static class MyClass1 implements MyIntf {
    @Override
    public @Self MyClass1 doSomethingAndReturnYourself(){
      // do something ...
      return this;
    }
  }

  public static class MyClass2 extends MyClass1 {
    // also inherits doSomethingAndReturnYourself method, but should now return MyClass2

    // --> ERROR
  }

  @Test
  public void testReturnInheritance() {
    MyClass2 myClass2 = new MyClass2().doSomethingAndReturnYourself();
  }
}
