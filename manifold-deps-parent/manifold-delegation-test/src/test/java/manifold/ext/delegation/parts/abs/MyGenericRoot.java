package manifold.ext.delegation.parts.abs;

import manifold.ext.delegation.rt.api.link;

public class MyGenericRoot<R extends CharSequence> implements Foo<R>
{
  @link AbstractFooPart<R> foo = AbstractFooPart.<R>asLink();

  @Override
  public R bar( R r )
  {
    return (R)(r + " : MyRoot.bar");
  }
}
