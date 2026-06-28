package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class DefOuter implements DefCalc
{
  @link DefCalc mid = new DefMid();
}
