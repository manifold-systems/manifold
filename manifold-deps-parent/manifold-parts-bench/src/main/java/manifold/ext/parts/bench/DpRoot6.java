package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class DpRoot6 implements DefCalc
{
  @link DefCalc outer = new DefOuter4();
  @Override public int scale( int x ) { return x * 2; }
}
