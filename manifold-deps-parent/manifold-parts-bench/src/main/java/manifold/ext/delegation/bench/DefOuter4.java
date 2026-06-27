package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

@part
public class DefOuter4 implements DefCalc
{
  @link DefCalc inner = new DefOuter3();
}
