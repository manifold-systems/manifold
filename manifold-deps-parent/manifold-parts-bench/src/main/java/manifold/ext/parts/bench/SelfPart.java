package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.part;

// CP leaf: compute() issues a self-call to scale(), which routes to the composite root.
@part
public class SelfPart implements Calc
{
  @Override public int compute( int x ) { return scale( x ); }  // -> $selves[IDX_Calc].scale(x)
  @Override public int scale( int x )   { return x; }
}
