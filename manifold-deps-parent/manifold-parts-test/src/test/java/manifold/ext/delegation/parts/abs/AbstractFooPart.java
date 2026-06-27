package manifold.ext.delegation.parts.abs;

import manifold.ext.delegation.rt.api.part;

@part
public abstract class AbstractFooPart<S extends CharSequence> implements Foo<S>
{
  private String _name;

//  public AbstractFooPart( String name )
//  {
//    _name = name;
//  }

  @Override
  public S foo( S s )
  {
    return (S)(s + " : AbstractFooPart.foo");
  }

  // unimplemented bar()
}
