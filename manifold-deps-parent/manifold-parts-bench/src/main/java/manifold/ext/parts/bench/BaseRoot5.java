package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class BaseRoot5 implements Calc
{
  @link Calc outer = new BaseOuter3();
}
