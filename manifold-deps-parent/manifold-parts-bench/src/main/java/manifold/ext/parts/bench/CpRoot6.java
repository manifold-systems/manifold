package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.link;

public class CpRoot6 implements Calc
{
  @link Calc outer = new SelfOuter4();
  @Override public int scale( int x ) { return x * 2; }
}
