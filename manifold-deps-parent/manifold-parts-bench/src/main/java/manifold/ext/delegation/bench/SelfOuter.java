package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

@part
public class SelfOuter implements Calc
{
  @link Calc mid = new SelfMid();
}
