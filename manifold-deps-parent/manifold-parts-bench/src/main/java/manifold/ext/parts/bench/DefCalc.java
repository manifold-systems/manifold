package manifold.ext.parts.bench;

// Like Calc, but compute() is an interface DEFAULT method whose body self-calls scale().
// A @part that does not override compute() inherits this default; invoking it routes through
// the generated invokedynamic override (Section 5.4), with the receiver taken from $selves.
public interface DefCalc
{
  default int compute( int x ) { return scale( x ); }   // self-call -> $selves[IDX_DefCalc].scale(x)
  int scale( int x );
}
