package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class CpRoot5 implements Calc
{
  @link Calc outer = new SelfOuter3();
  @Override public int scale( int x ) { return x * 2; }
}
