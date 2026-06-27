package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class BaseRoot1 implements Calc
{
  @link Calc part = new BasePart();
}
