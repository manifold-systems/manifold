package manifold.ext.delegation.parts.abs;

import manifold.ext.delegation.rt.api.link;

public class MyRoot implements Foo<String>
{
  @link AbstractFooPart<String> foo = AbstractFooPart.<String>asLink();

  @Override
  public String bar( String p )
  {
    return p + " : MyRoot.bar";
  }
}
