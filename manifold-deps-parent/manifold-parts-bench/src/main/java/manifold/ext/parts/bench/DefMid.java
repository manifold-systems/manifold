package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class DefMid implements DefCalc
{
  @link DefCalc inner = new DefPart();
}
