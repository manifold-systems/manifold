/*
 * Copyright (c) 2019 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.collections.api.range;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Defines binding functions to enable range expressions such as:
 * <p/>
 * {@code 5kg to 10kg}
 * <p/>
 * Mostly intended for use with {@code for} loops:
 * <pre><code>
 * for(int i: 1 to 10) {. . .}
 * for(Length len: 10ft to 100ft step 6 unit inch) {. . .}
 * for(var value: here to there) {. . .}
 * </code></pre>
 * To use this class simply import the {@code to} constants:
 * <pre><code>
 * import static manifold.collections.api.range.To.*;}
 * </code></pre>
 */
@SuppressWarnings("unused")
public final class RangeFun
{
  /**
   * To use these constants:
   * <pre><code>
   * import static manifold.collections.api.range.To.*;}
   * </code></pre>
   * Then you can make range expressions on {@link Sequential} values like this:
   * <pre><code>
   * for(int i: 1 to 10) {. . .}
   * for(Length len: 10ft to 100ft step 6 unit inch) {. . .}
   * for(Foo value: here to there) {. . .}
   * </code></pre>
   * Use the {@code in} constant to test for sequence membership:
   * <pre><code>
   * if(num in 2 to 10) {...}
   * // same as...
   * if(num >= 2 && num <= 10) {...}
   * // and same as...
   * if((2 to 10).contains(num)) {...}
   * </code></pre>
   */
  public static final Closed     to   = Closed.instance();    // both endpoints included
  public static final LeftOpen  _to   = LeftOpen.instance();  // left endpoint excluded
  public static final RightOpen  to_  = RightOpen.instance(); // right endpoint excluded
  public static final Open      _to_  = Open.instance();      // both endpoints excluded

  public static final Step      step  = Step.instance();      // provides a `step` clause
  public static final Unit      unit  = Unit.instance();      // provides a `unit` clause
  
  public static final Inside inside = Inside.instance();      // test for range membership
  public static final Outside outside = Outside.instance();   // negative test for range membership

  /**
   * For internal use.
   */
  public static class Closed
  {
    private static final Closed INSTANCE = new Closed();

    public static Closed instance()
    {
      return INSTANCE;
    }

    boolean _leftClosed;
    boolean _rightClosed;

    private Closed()
    {
      _leftClosed = true;
      _rightClosed = true;
    }

    /** Comparable */
    
    public <E extends Comparable<E>> From_Comp<E> postfixBind( E comparable )
    {
      return new From_Comp<>( comparable );
    }

    public class From_Comp<E extends Comparable<E>>
    {
      private E _start;

      From_Comp( E sequential )
      {
        _start = sequential;
      }

      public ComparableRange<E> prefixBind( E end )
      {
        return new ComparableRange<>( _start, end, _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }

    /** Sequential */

    public <E extends Sequential<E, S, U>, S, U> From_Seq<E, S, U> postfixBind( E sequential )
    {
      return new From_Seq<>( sequential );
    }

    public class From_Seq<E extends Sequential<E, S, U>, S, U>
    {
      private E _start;

      From_Seq( E sequential )
      {
        _start = sequential;
      }

      public SequentialRange<E, S, U> prefixBind( E end )
      {
        return new SequentialRange<>( _start, end, null, null, _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }

    /** BigDecimal */
    
    public From_BigDecimal postfixBind( BigDecimal bd )
    {
      return new From_BigDecimal( bd );
    }
    public class From_BigDecimal
    {
      private BigDecimal _start;

      From_BigDecimal( BigDecimal sequential )
      {
        _start = sequential;
      }

      public BigDecimalRange prefixBind( BigDecimal end )
      {
        return new BigDecimalRange( _start, end, BigDecimal.ONE,
          _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }

    /** BigInteger */
    
    public From_BigInteger postfixBind( BigInteger bd )
    {
      return new From_BigInteger( bd );
    }
    public class From_BigInteger
    {
      private BigInteger _start;

      From_BigInteger( BigInteger sequential )
      {
        _start = sequential;
      }

      public BigIntegerRange prefixBind( BigInteger end )
      {
        return new BigIntegerRange( _start, end, BigInteger.ONE,
          _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }

    /** Double */

    public From_Double postfixBind( Double bd )
    {
      return new From_Double( bd );
    }
    public class From_Double
    {
      private Double _start;

      From_Double( Double sequential )
      {
        _start = sequential;
      }

      public DoubleRange prefixBind( Double end )
      {
        return new DoubleRange( _start, end, 1,
          _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }

    /** Long */

    public From_Long postfixBind( Long bd )
    {
      return new From_Long( bd );
    }
    public class From_Long
    {
      private Long _start;

      From_Long( Long sequential )
      {
        _start = sequential;
      }

      public LongRange prefixBind( Long end )
      {
        return new LongRange( _start, end, 1,
          _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }

    /** Integer */
    
    public From_Integer postfixBind( Integer bd )
    {
      return new From_Integer( bd );
    }
    public class From_Integer
    {
      private Integer _start;

      From_Integer( Integer sequential )
      {
        _start = sequential;
      }

      public IntegerRange prefixBind( Integer end )
      {
        return new IntegerRange( _start, end, 1,
          _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }
  }

  /**
   * For internal use.
   */
  public static class LeftOpen extends Closed
  {
    private static final LeftOpen INSTANCE = new LeftOpen();

    public static LeftOpen instance()
    {
      return INSTANCE;
    }

    private LeftOpen()
    {
      _leftClosed = false;
    }
  }

  /**
   * For internal use.
   */
  public static class RightOpen extends Closed
  {
    private static final RightOpen INSTANCE = new RightOpen();

    public static RightOpen instance()
    {
      return INSTANCE;
    }

    private RightOpen()
    {
      _rightClosed = false;
    }
  }

  /**
   * For internal use.
   */
  public static class Open extends Closed
  {
    private static final Open INSTANCE = new Open();

    public static Open instance()
    {
      return INSTANCE;
    }

    private Open()
    {
      _leftClosed = false;
      _rightClosed = false;
    }
  }

  public static class Step
  {
    private static final Step INSTANCE = new Step();

    public static Step instance()
    {
      return INSTANCE;
    }

    public <E extends Comparable<E>, S, U, RANGE extends AbstractIterableRange<E, S, U, RANGE>> StepRange<E, S, U, RANGE> postfixBind( RANGE range )
    {
      return new StepRange<>( range );
    }

    public static class StepRange<E extends Comparable<E>, S, U, RANGE extends AbstractIterableRange<E, S, U, RANGE>>
    {
      private final RANGE _range;

      StepRange( RANGE range )
      {
        _range = range;
      }
      
      public RANGE prefixBind( S step )
      {
        return _range.step( step );
      }
    }
  }

  public static class Unit
  {
    private static final Unit INSTANCE = new Unit();

    public static Unit instance()
    {
      return INSTANCE;
    }

    public <E extends Comparable<E>, S, U, RANGE extends AbstractIterableRange<E, S, U, RANGE>> UnitRange<E, S, U, RANGE> postfixBind( RANGE range )
    {
      return new UnitRange<>( range );
    }

    public static class UnitRange<E extends Comparable<E>, S, U, RANGE extends AbstractIterableRange<E, S, U, RANGE>>
    {
      private final RANGE _range;

      UnitRange( RANGE range )
      {
        _range = range;
      }
      
      public RANGE prefixBind( U unit )
      {
        return _range.unit( unit );
      }
    }
  }
  
  public static class Inside
  {
    private static final Inside INSTANCE = new Inside();

    public static Inside instance()
    {
      return INSTANCE;
    }

    public <E extends Comparable<E>, RANGE extends AbstractRange<E, RANGE>> InsideRange<E, RANGE> prefixBind( RANGE range )
    {
      return new InsideRange<>( range );
    }

    public static class InsideRange<E extends Comparable<E>, RANGE extends AbstractRange<E, RANGE>>
    {
      private final RANGE _range;

      InsideRange( RANGE range )
      {
        _range = range;
      }
      
      public boolean postfixBind( E element )
      {
        return _range.contains( element );
      }
    }
  }
  
  public static class Outside 
  {
    private static final Outside INSTANCE = new Outside();

    public static Outside instance()
    {
      return INSTANCE;
    }

    public <E extends Comparable<E>, RANGE extends AbstractRange<E, RANGE>> OutsideRange<E, RANGE> prefixBind( RANGE range )
    {
      return new OutsideRange<>( range );
    }

    public static class OutsideRange<E extends Comparable<E>, RANGE extends AbstractRange<E, RANGE>>
    {
      private final RANGE _range;

      OutsideRange( RANGE range )
      {
        _range = range;
      }

      public boolean postfixBind( E element )
      {
        return !_range.contains( element );
      }
    }
  }
}