package manifold.ext.delegation.bench;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Measures interface-scoped dispatch cost relative to standard Java interface dispatch.
 *
 * Design:
 *   baseline_depthN  — N @link delegation hops to reach a leaf that returns directly (no self-call)
 *   cp_depthN        — same N hops to the same leaf depth, but the leaf calls scale() via self-call
 *                      ($selves[IDX_Calc].scale(x)), which routes to the root's override
 *
 * delta = cp_depthN - baseline_depthN
 *       = cost of exactly one self-call dispatch ($selves[IDX] lookup + invokeinterface)
 *
 * If delta is constant across depths, self-call dispatch is O(1).
 * If delta ≈ baseline_depth1 (one plain interface dispatch), the cost is equivalent to vtable.
 *
 * Build:  mvn -pl :manifold-delegation-bench package   (from manifold-deps-parent)
 * Run:    java -jar target/benchmarks.jar
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class DispatchBenchmark
{
  // Single virtual dispatch anchor — the unit cost we compare against.
  private Calc direct;

  // Baseline: N delegation hops, no self-call.
  private Calc base1;
  private Calc base2;
  private Calc base3;
  private Calc base4;
  private Calc base5;
  private Calc base6;

  // CP: N delegation hops + one self-call ($selves[IDX].scale) routing to root.
  private Calc cp1;
  private Calc cp2;
  private Calc cp3;
  private Calc cp4;
  private Calc cp5;
  private Calc cp6;

  @Setup
  public void setup()
  {
    direct = new DirectCalc();
    base1  = new BaseRoot1();
    base2  = new BaseRoot2();
    base3  = new BaseRoot3();
    base4  = new BaseRoot4();
    base5  = new BaseRoot5();
    base6  = new BaseRoot6();
    cp1    = new CpRoot1();
    cp2    = new CpRoot2();
    cp3    = new CpRoot3();
    cp4    = new CpRoot4();
    cp5    = new CpRoot5();
    cp6    = new CpRoot6();
  }

  // One plain interface dispatch — unit cost baseline.
  @Benchmark
  public int direct()          { return direct.scale( 7 ); }

  // 1 delegation hop, no self-call.
  @Benchmark
  public int baseline_depth1() { return base1.compute( 7 ); }

  // 2 delegation hops, no self-call.
  @Benchmark
  public int baseline_depth2() { return base2.compute( 7 ); }

  // 3 delegation hops, no self-call.
  @Benchmark
  public int baseline_depth3() { return base3.compute( 7 ); }

  // 4 delegation hops, no self-call.
  @Benchmark
  public int baseline_depth4() { return base4.compute( 7 ); }

  // 5 delegation hops, no self-call.
  @Benchmark
  public int baseline_depth5() { return base5.compute( 7 ); }

  // 6 delegation hops, no self-call.
  @Benchmark
  public int baseline_depth6() { return base6.compute( 7 ); }

  // 1 delegation hop + self-call dispatch.
  @Benchmark
  public int cp_depth1()       { return cp1.compute( 7 ); }

  // 2 delegation hops + self-call dispatch.
  @Benchmark
  public int cp_depth2()       { return cp2.compute( 7 ); }

  // 3 delegation hops + self-call dispatch.
  @Benchmark
  public int cp_depth3()       { return cp3.compute( 7 ); }

  // 4 delegation hops + self-call dispatch.
  @Benchmark
  public int cp_depth4()       { return cp4.compute( 7 ); }

  // 5 delegation hops + self-call dispatch.
  @Benchmark
  public int cp_depth5()       { return cp5.compute( 7 ); }

  // 6 delegation hops + self-call dispatch.
  @Benchmark
  public int cp_depth6()       { return cp6.compute( 7 ); }
}
