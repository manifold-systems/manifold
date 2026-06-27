package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.link;

public class DpRoot3 implements DefCalc
{
  @link DefCalc outer = new DefOuter();
  @Override public int scale( int x ) { return x * 2; }
}
