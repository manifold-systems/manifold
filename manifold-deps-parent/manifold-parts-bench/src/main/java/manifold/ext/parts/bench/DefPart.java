package manifold.ext.parts.bench;

import manifold.ext.parts.rt.api.part;

// DP leaf: compute() is NOT overridden -> inherits DefCalc's default -> generated invokedynamic
// override invokes the default body with $selves[IDX_DefCalc] as receiver; the default's self-call
// to scale() then routes to the composite root.
@part
public class DefPart implements DefCalc
{
  @Override public int scale( int x ) { return x; }
}
