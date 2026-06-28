package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class DpRoot5 implements DefCalc
{
  @link DefCalc outer = new DefOuter3();
  @Override public int scale( int x ) { return x * 2; }
}
