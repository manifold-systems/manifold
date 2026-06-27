package manifold.ext.delegation.bench;

import manifold.ext.delegation.rt.api.part;

// Baseline leaf: compute() returns directly, no self-call.
@part
public class BasePart implements Calc
{
  @Override public int compute( int x ) { return x * 2; }
  @Override public int scale( int x )   { return x * 2; }
}
