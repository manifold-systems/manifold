package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class BaseMid implements Calc
{
  @link Calc inner = new BasePart();
}
