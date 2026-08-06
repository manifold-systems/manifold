package manifold.ext.parts.parts.abs;

import manifold.ext.parts.rt.api.link;

public class MyRoot implements Foo<String>
{
  @link Foo<String> foo = AbstractFooPart.asLink();

  @Override
  public String bar( String p )
  {
    return p + " : MyRoot.bar";
  }
}
