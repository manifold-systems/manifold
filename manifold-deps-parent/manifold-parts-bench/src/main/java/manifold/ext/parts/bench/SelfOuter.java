package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class SelfOuter implements Calc
{
  @link Calc mid = new SelfMid();
}
