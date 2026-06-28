package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

@part
public class BaseOuter4 implements Calc
{
  @link Calc inner = new BaseOuter3();
}
