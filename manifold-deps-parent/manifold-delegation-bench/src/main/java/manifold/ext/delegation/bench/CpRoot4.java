package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class CpRoot4 implements Calc
{
  @link Calc outer = new SelfOuter2();
  @Override public int scale( int x ) { return x * 2; }
}
