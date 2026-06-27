package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class BaseRoot2 implements Calc
{
  @link Calc mid = new BaseMid();
}
