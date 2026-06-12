package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

@part
public class BaseOuter4 implements Calc
{
  @link Calc inner = new BaseOuter3();
}
