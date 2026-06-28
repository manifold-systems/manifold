package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class SelfOuter4 implements Calc
{
  @link Calc inner = new SelfOuter3();
}
