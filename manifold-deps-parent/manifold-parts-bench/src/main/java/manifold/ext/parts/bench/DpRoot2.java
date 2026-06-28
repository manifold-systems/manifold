package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class DpRoot2 implements DefCalc
{
  @link DefCalc mid = new DefMid();
  @Override public int scale( int x ) { return x * 2; }
}
