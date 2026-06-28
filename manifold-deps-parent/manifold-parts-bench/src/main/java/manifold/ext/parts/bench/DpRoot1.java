package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class DpRoot1 implements DefCalc
{
  @link DefCalc part = new DefPart();
  @Override public int scale( int x ) { return x * 2; }  // self-call target; no further delegation
}
