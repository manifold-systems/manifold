package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class BaseRoot2 implements Calc
{
  @link Calc mid = new BaseMid();
}
