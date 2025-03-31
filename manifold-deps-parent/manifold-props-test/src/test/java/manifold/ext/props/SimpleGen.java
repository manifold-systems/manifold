package manifold.ext.props;

import manifold.ext.props.rt.api.var;

public class SimpleGen
{
  static class Parent<T> {
    @var T foo;
  }

  static class Child extends Parent<String> {
    public String testMe() {
      return foo; // tests that foo's instance resolves properly
    }
  }
}
