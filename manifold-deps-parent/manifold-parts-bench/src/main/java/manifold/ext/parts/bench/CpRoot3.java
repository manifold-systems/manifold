package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class CpRoot3 implements Calc
{
  @link Calc outer = new SelfOuter();
  @Override public int scale( int x ) { return x * 2; }  // self-call target; no further delegation
}
