package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class CpRoot5 implements Calc
{
  @link Calc outer = new SelfOuter3();
  @Override public int scale( int x ) { return x * 2; }
}
