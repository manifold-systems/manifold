package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class BaseRoot3 implements Calc
{
  @link Calc outer = new BaseOuter();
}
