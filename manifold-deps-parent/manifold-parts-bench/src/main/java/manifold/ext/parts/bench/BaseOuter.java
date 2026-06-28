package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class BaseOuter implements Calc
{
  @link Calc mid = new BaseMid();
}
