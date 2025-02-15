package manifold.ext;


import manifold.ext.sup.SuperClass2;

public class SubClass2 extends SuperClass2
{
  public DeferredJoiner foo()
  {
    DeferredJoiner deferredJoiner1 = DeferredJoiner.combine( SubClass2.super::foo, () -> "test" );
    DeferredJoiner deferredJoiner2 = DeferredJoiner.combine( super::foo, () -> "test2" );
    return deferredJoiner1.add( deferredJoiner2::get );
  }
}
