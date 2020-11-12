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

package manifold.science.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import manifold.ext.rt.api.ComparableUsing;
import manifold.collections.api.range.Sequential;
import manifold.science.measures.MetricScaleUnit;

/**
 * Models rational numbers as an immutable fraction using {@link BigInteger} to maintain arbitrary precision. Note as a
 * performance measure this class does <i>not</i> maintain its value in reduced form. You must call {@link #reduce()}
 * to get a separate instance for the reduced form. Call {@link #isReduced()} to determine if an instance is in reduced
 * form.
 * <p/>
 * This class implements arithmetic, negation, and relational operators via <i>operator overloading</i> provided by the
 * manifold-ext dependency.
 * <p/>
 * Use the {@link CoercionConstants} and {@link MetricScaleUnit} classes to conveniently use literal
 * values as {@code Rational} numbers:
 * <pre><code>
 *   import static manifold.science.measures.MetricScaleUnit.M;
 *   import static manifold.science.util.CoercionConstants.r;
 *   ...
 *   Rational pi = 3.14r;
 *   Rational oneThird = 1r/3;
 *   Rational yocto = "1/1000000000000000000000000"r;
 *   Rational fiveMillion = 5M;
 * </code></pre>
 * <b>WARNING:</b> this class is under development and should be considered experimental.
 */
final public class Rational extends Number implements Sequential<Rational, Rational, Void>, ComparableUsing<Rational>, Serializable
{
  private static final int VERSION_1 = 1;

  public static final Rational ZERO = new Rational( BigInteger.ZERO, BigInteger.ONE, true );
  public static final Rational ONE = new Rational( BigInteger.ONE, BigInteger.ONE, true );
  public static final Rational TEN = new Rational( BigInteger.valueOf( 10 ), BigInteger.ONE, true );
  public static final Rational HALF = new Rational( BigInteger.ONE, BigInteger.valueOf( 2 ), true );

  private final BigInteger _numerator;
  private final BigInteger _denominator;
  private boolean _reduced;

  public static Rational get( int numerator )
  {
    return get( BigInteger.valueOf( numerator ), BigInteger.ONE );
  }

  public static Rational get( int numerator, int denominator )
  {
    return get( BigInteger.valueOf( numerator ), BigInteger.valueOf( denominator ) );
  }

  public static Rational get( long numerator )
  {
    return get( BigInteger.valueOf( numerator ), BigInteger.ONE );
  }

  public static Rational get( long numerator, long denominator )
  {
    return get( BigInteger.valueOf( numerator ), BigInteger.valueOf( denominator ) );
  }

  public static Rational get( float f )
  {
    return get( Float.toString( f ) );
  }

  public static Rational get( double d )
  {
    return get( Double.toString( d ) );
  }

  public static Rational get( BigInteger numerator )
  {
    return get( numerator, BigInteger.ONE );
  }

  public static Rational get( Number numerator )
  {
    return get( String.valueOf( numerator ) );
  }

  public static Rational get( String decimal )
  {
    int iDiv = decimal.indexOf( "/" );
    if( iDiv > 0 )
    {
      String numerator = decimal.substring( 0, iDiv ).trim();
      String denominator = decimal.substring( iDiv + 1 ).trim();
      boolean numeratorIsDecimal = isDecimalString( numerator );
      boolean denominatorIsDecimal = isDecimalString( denominator );
      if( numeratorIsDecimal )
      {
        if( denominatorIsDecimal )
        {
          return get( new BigDecimal( numerator ) ).div( get( new BigDecimal( denominator ) ) );
        }
        return get( new BigDecimal( numerator ) ).div( new BigInteger( denominator ) );
      }
      else if( denominatorIsDecimal )
      {
        return get( new BigInteger( numerator ) ).div( get( new BigDecimal( denominator ) ) );
      }
      return get( new BigInteger( numerator ), new BigInteger( denominator ) );
    }
    else
    {
      if( isDecimalString( decimal ) )
      {
        return get( new BigDecimal( decimal ) );
      }
      return get( new BigInteger( decimal ) );
    }
  }

  private static boolean isDecimalString( String decimal )
  {
    return decimal.indexOf( '.' ) >= 0 ||
           decimal.indexOf( 'e' ) > 0 ||
           decimal.indexOf( 'E' ) > 0;
  }

  public static Rational get( BigDecimal bd )
  {
    int scale = bd.scale();

    BigInteger numerator;
    BigInteger denominator;
    if( scale >= 0 )
    {
      numerator = bd.unscaledValue();
      denominator = BigInteger.TEN.pow( scale );
    }
    else
    {
      numerator = bd.unscaledValue().multiply( BigInteger.TEN.pow( -scale ) );
      denominator = BigInteger.ONE;
    }
    return get( numerator, denominator );
  }

  public static Rational get( BigInteger numerator, BigInteger denominator )
  {
    return get( numerator, denominator, false );
  }

  private static Rational get( BigInteger numerator, BigInteger denominator, boolean reduced )
  {
    if( numerator.equals( BigInteger.ZERO ) )
    {
      return ZERO;
    }
    if( numerator.equals( BigInteger.ONE ) && denominator.equals( BigInteger.ONE ) )
    {
      return ONE;
    }
    return new Rational( numerator, denominator, reduced );
  }

  private Rational( BigInteger numerator, BigInteger denominator, boolean reduced )
  {
    if( denominator.signum() == 0 )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    if( numerator.signum() == 0 )
    {
      _numerator = BigInteger.ZERO;
      _denominator = BigInteger.ONE;
    }
    else
    {
      if( denominator.signum() == -1 )
      {
        numerator = numerator.negate();
        denominator = denominator.negate();
      }

      _numerator = numerator;
      _denominator = denominator;
    }
    _reduced = reduced;
  }

  /**
   * @return {@code true} if this instance is in reduced form.
   */
  public boolean isReduced()
  {
    // calling reduce() has a side effect of setting _reduced to true if this instance is found to be in reduced form
    return _reduced || reduce().isReduced() && _reduced;
  }

  /**
   * @return If this instance is already in reduced form, returns this instance, otherwise returns the reduced form of
   * this instance as a separate instance.
   */
  public Rational reduce()
  {
    if( !_reduced )
    {
      BigInteger gcd = _numerator.gcd( _denominator );
      if( gcd.compareTo( BigInteger.ONE ) > 0 )
      {
        return get( _numerator.divide( gcd ), _denominator.divide( gcd ), true );
      }
      _reduced = true;
    }
    return this;
  }

  public BigInteger getNumerator()
  {
    return _numerator;
  }

  public BigInteger getDenominator()
  {
    return _denominator;
  }

  public BigInteger wholePart()
  {
    return _numerator.divide( _denominator );
  }

  public Rational fractionPart()
  {
    BigInteger rem = _numerator.remainder( _denominator );
    if( rem.signum() == 0 )
    {
      return ZERO;
    }
    return Rational.get( rem, _denominator );
  }

  @Override
  public int intValue()
  {
    return _numerator.divide( _denominator ).intValue();
  }

  @Override
  public long longValue()
  {
    return _numerator.divide( _denominator ).longValue();
  }

  @Override
  public double doubleValue()
  {
    return toBigDecimal().doubleValue();
  }

  @Override
  public float floatValue()
  {
    return toBigDecimal().floatValue();
  }

  public BigInteger toBigInteger()
  {
    return toBigDecimal().toBigInteger();
  }

  public BigDecimal toBigDecimal()
  {
    return toBigDecimal( MathContext.DECIMAL128 );
  }

  public BigDecimal toBigDecimal( MathContext mc )
  {
    return equals( ZERO )
           ? BigDecimal.ZERO
           : new BigDecimal( _numerator ).divide( new BigDecimal( _denominator ), mc );
  }

  public Rational plus( int i )
  {
    return i == 0 ? this : plus( get( i ) );
  }

  public Rational plus( long l )
  {
    return l == 0 ? this : plus( get( l ) );
  }

  public Rational plus( float f )
  {
    return f == 0 ? this : plus( get( f ) );
  }

  public Rational plus( double d )
  {
    return d == 0 ? this : plus( get( d ) );
  }

  public Rational plus( BigInteger bg )
  {
    if( signum() == 0 )
    {
      return get( bg );
    }
    if( bg.signum() == 0 )
    {
      return this;
    }

    return bg.equals( BigInteger.ZERO )
           ? this
           : get( _numerator.add( _denominator.multiply( bg ) ), _denominator );
  }

  public Rational plus( BigDecimal bd )
  {
    return bd.signum() == 0 ? this : plus( get( bd ) );
  }

  public Rational plus( Rational rational )
  {
    if( rational.signum() == 0 )
    {
      return this;
    }
    if( signum() == 0 )
    {
      return rational;
    }

    BigInteger numerator;
    BigInteger denominator;

    if( _denominator.equals( rational._denominator ) )
    {
      numerator = _numerator.add( rational._numerator );
      denominator = _denominator;
    }
    else
    {
      numerator = (_numerator.multiply( rational._denominator )).add( (rational._numerator).multiply( _denominator ) );
      denominator = _denominator.multiply( rational._denominator );
    }

    return numerator.signum() == 0
           ? ZERO
           : get( numerator, denominator );
  }

  public Rational plus( Number n )
  {
    if( n instanceof Rational )
    {
      return plus( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return plus( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return plus( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return plus( (double)n );
    }
    else if( n instanceof Float )
    {
      return plus( (float)n );
    }
    else if( n instanceof Integer )
    {
      return plus( (int)n );
    }
    else if( n instanceof Long )
    {
      return plus( (long)n );
    }
    else
    {
      return plus( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational minus( int i )
  {
    return minus( BigInteger.valueOf( i ) );
  }

  public Rational minus( long l )
  {
    return minus( BigInteger.valueOf( l ) );
  }

  public Rational minus( float f )
  {
    return minus( get( f ) );
  }

  public Rational minus( double d )
  {
    return minus( get( d ) );
  }

  public Rational minus( BigInteger bi )
  {
    if( bi.signum() == 0 )
    {
      return this;
    }
    if( signum() == 0 )
    {
      return get( bi.negate() );
    }
    return get( _numerator.subtract( _denominator.multiply( bi ) ), _denominator );
  }

  public Rational minus( BigDecimal bd )
  {
    return bd.signum() == 0 ? this : minus( get( bd ) );
  }

  public Rational minus( Rational rational )
  {
    if( rational.signum() == 0 )
    {
      return this;
    }
    if( signum() == 0 )
    {
      return rational.unaryMinus();
    }

    BigInteger numerator;
    BigInteger denominator;
    if( _denominator.equals( rational._denominator ) )
    {
      numerator = _numerator.subtract( rational._numerator );
      denominator = _denominator;
    }
    else
    {
      numerator = (_numerator.multiply( rational._denominator )).subtract( (rational._numerator).multiply( _denominator ) );
      denominator = _denominator.multiply( rational._denominator );
    }
    return numerator.signum() == 0
           ? ZERO
           : get( numerator, denominator );
  }

  public Rational minus( Number n )
  {
    if( n instanceof Rational )
    {
      return minus( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return minus( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return minus( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return minus( (double)n );
    }
    else if( n instanceof Float )
    {
      return minus( (float)n );
    }
    else if( n instanceof Integer )
    {
      return minus( (int)n );
    }
    else if( n instanceof Long )
    {
      return minus( (long)n );
    }
    else
    {
      return minus( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational times( int i )
  {
    if( i == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return times( BigInteger.valueOf( i ) );
  }

  public Rational times( long l )
  {
    if( l == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return times( BigInteger.valueOf( l ) );
  }

  public Rational times( float f )
  {
    if( f == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return times( get( f ) );
  }

  public Rational times( double d )
  {
    if( d == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return times( get( d ) );
  }

  public Rational times( BigInteger bi )
  {
    if( signum() == 0 || bi.signum() == 0 )
    {
      return ZERO;
    }
    return get( bi.multiply( _numerator ), _denominator );
  }

  public Rational times( BigDecimal bd )
  {
    if( signum() == 0 || bd.signum() == 0 )
    {
      return ZERO;
    }
    return times( get( bd ) );
  }

  public Rational times( Rational rational )
  {
    if( signum() == 0 || rational.signum() == 0 )
    {
      return ZERO;
    }
    return get( _numerator.multiply( rational._numerator ),
      _denominator.multiply( rational._denominator ) );
  }

  public Rational times( Number n )
  {
    if( n instanceof Rational )
    {
      return times( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return times( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return times( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return times( (double)n );
    }
    else if( n instanceof Float )
    {
      return times( (float)n );
    }
    else if( n instanceof Integer )
    {
      return times( (int)n );
    }
    else if( n instanceof Long )
    {
      return times( (long)n );
    }
    else
    {
      return times( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational div( int i )
  {
    return div( BigInteger.valueOf( i ) );
  }

  public Rational div( long l )
  {
    return div( BigInteger.valueOf( l ) );
  }

  public Rational div( float f )
  {
    return div( get( f ) );
  }

  public Rational div( double d )
  {
    return div( get( d ) );
  }

  public Rational div( BigInteger bi )
  {
    if( bi.equals( BigInteger.ZERO ) )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    if( signum() == 0 )
    {
      return ZERO;
    }
    return get( _numerator, _denominator.multiply( bi ) );
  }

  public Rational div( BigDecimal bd )
  {
    if( bd.signum() == 0 )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    if( signum() == 0 )
    {
      return ZERO;
    }
    return div( get( bd ) );
  }

  public Rational div( Rational rational )
  {
    if( rational.equals( ZERO ) )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    if( signum() == 0 )
    {
      return ZERO;
    }
    return times( rational.invert() );
  }

  public Rational div( Number n )
  {
    if( n instanceof Rational )
    {
      return div( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return div( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return div( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return div( (double)n );
    }
    else if( n instanceof Float )
    {
      return div( (float)n );
    }
    else if( n instanceof Integer )
    {
      return div( (int)n );
    }
    else if( n instanceof Long )
    {
      return div( (long)n );
    }
    else
    {
      return div( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational rem( int i )
  {
    return rem( BigInteger.valueOf( i ) );
  }

  public Rational rem( long l )
  {
    return rem( BigInteger.valueOf( l ) );
  }

  public Rational rem( float f )
  {
    return rem( get( f ) );
  }

  public Rational rem( double d )
  {
    return rem( get( d ) );
  }

  public Rational rem( BigInteger bi )
  {
    if( bi.equals( BigInteger.ZERO ) )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    return rem( get( bi ) );
  }

  public Rational rem( BigDecimal bd )
  {
    if( bd.signum() == 0 )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    return rem( get( bd ) );
  }

  public Rational rem( Rational rational )
  {
    Rational quotient = div( rational );
    return minus( rational.times( quotient.toBigInteger() ) ).abs();
  }

  public Rational rem( Number n )
  {
    if( n instanceof Rational )
    {
      return rem( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return rem( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return rem( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return rem( (double)n );
    }
    else if( n instanceof Float )
    {
      return rem( (float)n );
    }
    else if( n instanceof Integer )
    {
      return rem( (int)n );
    }
    else if( n instanceof Long )
    {
      return rem( (long)n );
    }
    else
    {
      return rem( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational unaryMinus()
  {
    return get( _numerator.negate(), _denominator );
  }

  /**
   * Supports unary increment operator {@code ++}
   */
  public Rational inc()
  {
    return plus( ONE );
  }

  /**
   * Supports unary decrement operator {@code --}
   */
  public Rational dec()
  {
    return minus( ONE );
  }

  public Rational invert()
  {
    return get( _denominator, _numerator );
  }

  public Rational abs()
  {
    return signum() >= 0 ? this : unaryMinus();
  }

  public Rational pow( int exponent )
  {
    if( signum() == 0 )
    {
      return exponent == 0 ? ONE : this;
    }
    return Rational.get( _numerator.pow( exponent ), _denominator.pow( exponent ) );
  }

  @Deprecated
  public Rational sqrt()
  {
    //todo: a rational impl
    return Rational.get( Math.sqrt( doubleValue() ) );
  }

  public boolean isInteger()
  {
    return _denominator.equals( BigInteger.ONE );
  }

  @Override
  public Rational nextInSequence( Rational step, Void unit )
  {
    step = step == null ? Rational.ONE : step;
    return plus( step );
  }

  @Override
  public Rational nextNthInSequence( Rational step, Void unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    return plus( step.times( index ) );
  }

  @Override
  public Rational previousInSequence( Rational step, Void unit )
  {
    step = step == null ? Rational.ONE : step;
    return minus( step );
  }

  @Override
  public Rational previousNthInSequence( Rational step, Void unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    return minus( step.times( index ) );
  }

  public int signum()
  {
    return _numerator.signum();
  }

  @Override
  public int compareTo( Rational that )
  {
    if( that == null )
    {
      throw new NullPointerException( "Null argument for comparison" );
    }

    // note: cast to Object to avoid using Rational's operator impl on == which is this method (avoids stack overflow)
    //noinspection RedundantCast
    if( (Object)this == (Object)that )
    {
      return 0;
    }

    int thisSign = signum();
    int thatSign = that.signum();
    if( thisSign != thatSign || thisSign == 0 )
    {
      return thisSign - thatSign;
    }
    BigInteger crossNum = _numerator.multiply( that._denominator );
    BigInteger crossDen = _denominator.multiply( that._numerator );
    return crossNum.compareTo( crossDen );
  }

  /**
   * Use {@code compareTo()} for {@code ==} and {@code !=} operators.
   */
  @Override
  public EqualityMode equalityMode()
  {
    return EqualityMode.CompareTo;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    Rational me = reduce();
    Rational that = ((Rational)o).reduce();
    if( !me._denominator.equals( that._denominator ) )
    {
      return false;
    }
    return me._numerator.equals( that._numerator );
  }

  @Override
  public int hashCode()
  {
    reduce();
    int result = _numerator.hashCode();
    result = 31 * result + _denominator.hashCode();
    return result;
  }

  public String toFractionString()
  {
    if( !_reduced )
    {
      return reduce().toFractionString();
    }
    return _numerator + "/" + _denominator;
  }

  public String toMixedString()
  {
    if( !_reduced )
    {
      return reduce().toMixedString();
    }

    if( _denominator.equals( BigInteger.ONE ) )
    {
      return _numerator.toString();
    }
    BigInteger whole = wholePart();
    if( whole.signum() == 0 )
    {
      return fractionPart().toFractionString();
    }
    return whole + " " + fractionPart().abs().toFractionString();
  }

  /**
   * Creates a {@link BigDecimal} and calls {@link BigDecimal#toString()}
   */
  public String toDecimalString()
  {
    return toBigDecimal().toString();
  }

  /**
   * Creates a {@link BigDecimal} and calls {@link BigDecimal#toPlainString()}
   */
  public String toPlainDecimalString()
  {
    return toBigDecimal().toPlainString();
  }

  /**
   * Creates a {@link BigDecimal} and calls {@link BigDecimal#toEngineeringString()}
   */
  public String toEngineeringString()
  {
    return toBigDecimal().toEngineeringString();
  }

  @Override
  public String toString()
  {
    return _numerator + " / " + _denominator;
  }

  private Object writeReplace()
  {
    return new Serializer( this );
  }

  private static class Serializer implements Externalizable
  {
    private Rational _rational;

    public Serializer()
    {
    }

    public Serializer( Rational rational )
    {
      _rational = rational;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
      out.writeInt( VERSION_1 );
      out.writeObject( _rational._numerator );
      out.writeObject( _rational._denominator );
      out.writeBoolean( _rational._reduced );
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
      int version = in.readInt();
      switch( version )
      {
        case VERSION_1:
          BigInteger numerator = (BigInteger)in.readObject();
          BigInteger denominator = (BigInteger)in.readObject();
          boolean reduced = in.readBoolean();
          _rational = get( numerator, denominator, reduced );
          break;

        default:
          throw new IllegalStateException( "Unsupported version: " + version );
      }
    }

    Object readResolve()
    {
      return _rational;
    }
  }
}