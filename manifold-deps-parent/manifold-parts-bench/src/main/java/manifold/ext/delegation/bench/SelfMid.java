package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

@part
public class SelfMid implements Calc
{
  @link Calc inner = new SelfPart();
}
