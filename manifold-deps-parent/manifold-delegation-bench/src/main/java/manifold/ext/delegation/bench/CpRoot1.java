package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class CpRoot1 implements Calc
{
  @link Calc part = new SelfPart();
  @Override public int scale( int x ) { return x * 2; }  // self-call target; no further delegation
}
