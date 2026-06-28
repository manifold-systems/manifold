package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class BaseRoot6 implements Calc
{
  @link Calc outer = new BaseOuter4();
}
