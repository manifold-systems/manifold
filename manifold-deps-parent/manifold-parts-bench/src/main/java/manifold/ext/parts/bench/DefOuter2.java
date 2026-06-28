package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class DefOuter2 implements DefCalc
{
  @link DefCalc inner = new DefOuter();
}
