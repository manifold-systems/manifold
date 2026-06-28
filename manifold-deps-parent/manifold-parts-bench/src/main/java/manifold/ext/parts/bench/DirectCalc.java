package manifold.ext.parts.bench;

public class DirectCalc implements Calc
{
  @Override public int compute( int x ) { return x; }
  @Override public int scale( int x )   { return x * 2; }
}
