package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class BaseRoot4 implements Calc
{
  @link Calc outer = new BaseOuter2();
}
