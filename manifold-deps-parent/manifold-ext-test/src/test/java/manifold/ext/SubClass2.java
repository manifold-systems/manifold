package manifold.ext;


import manifold.ext.sup.SuperClass2;

public class SubClass2 extends SuperClass2
{
  public DeferredJoiner foo()
  {
    return DeferredJoiner.combine( super::foo, () -> "test");
  }
}
