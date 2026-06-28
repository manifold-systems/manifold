package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class DpRoot3 implements DefCalc
{
  @link DefCalc outer = new DefOuter();
  @Override public int scale( int x ) { return x * 2; }
}
