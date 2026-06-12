package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class CpRoot3 implements Calc
{
  @link Calc outer = new SelfOuter();
  @Override public int scale( int x ) { return x * 2; }  // self-call target; no further delegation
}
