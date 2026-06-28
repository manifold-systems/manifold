package manifold.ext.parts.bench;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Isolates the dispatch mechanism alone, with the self-called target made
 * non-inlinable (@CompilerControl.DONT_INLINE) so a real indirect call happens
 * on every path. Depth 0 — no delegation hops — so the only difference between
 * benchmarks is the dispatch expression itself.
 *
 *   vtable_invokevirtual   : tgt.scale(x)            — invokevirtual, the inheritance self-call shape
 *   vtable_invokeinterface : ((Calc)tgt).scale(x)    — invokeinterface, same receiver via interface ref
 *   isd_array_interface    : ((Calc)$selves[0]).scale(x) — the exact shape the plugin emits
 *                            (Object[] element load + checkcast + invokeinterface)
 *
 * isd_array_interface - vtable_invokeinterface  isolates the ISD-specific cost:
 * the array-element load + cast over a plain interface dispatch. If it is ~0,
 * the array load overlaps with the call latency and ISD is equivalent to a
 * vtable call in the non-inlined regime.
 *
 * $selves is emulated as a final Object[] holding the wired receiver — identical
 * to what the plugin generates for a @part (see paper, Rewriting: ((I)$selves[IDX]).m()).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class DispatchIsolation
{
  // Non-inlinable target. Trivial body so call overhead + dispatch dominate, not the body.
  public static final class Tgt implements Calc
  {
    @Override public int compute( int x ) { return x; }
    @Override @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int scale( int x ) { return x * 2; }
  }

  private Tgt          concrete;   // invokevirtual
  private Calc         iface;      // invokeinterface
  private final Object[] selves = new Object[1];  // emulated $selves slot

  @Setup
  public void setup()
  {
    Tgt t = new Tgt();
    concrete = t;
    iface    = t;
    selves[0] = t;
  }

  // invokevirtual on a concrete receiver — the inheritance self-call shape.
  @Benchmark
  public int vtable_invokevirtual()   { return concrete.scale( 7 ); }

  // invokeinterface on the same receiver via an interface reference.
  @Benchmark
  public int vtable_invokeinterface() { return iface.scale( 7 ); }

  // The plugin's emitted form: Object[] element load + cast + invokeinterface.
  @Benchmark
  public int isd_array_interface()    { return ((Calc) selves[0]).scale( 7 ); }
}
