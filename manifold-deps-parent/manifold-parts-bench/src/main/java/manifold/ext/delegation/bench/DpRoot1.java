package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class DpRoot1 implements DefCalc
{
  @link DefCalc part = new DefPart();
  @Override public int scale( int x ) { return x * 2; }  // self-call target; no further delegation
}
