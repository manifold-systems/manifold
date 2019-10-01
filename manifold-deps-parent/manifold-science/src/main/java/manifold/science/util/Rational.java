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
import manifold.ext.api.IComparableWith;
import manifold.science.api.ISequenceable;

/**
 * Models rational number as a fraction to maintain arbitrary precision.
 */
final public class Rational extends Number implements ISequenceable<Rational, Rational, Void>, IComparableWith<Rational>, Serializable
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
      String denominator = decimal.substring( iDiv+1 ).trim();
      boolean numeratorIsDecimal = isDecimalString( numerator );
      boolean denominatorIsDecimal = isDecimalString( denominator );
      if( numeratorIsDecimal )
      {
        if( denominatorIsDecimal )
        {
          return get( new BigDecimal( numerator ) ).divide( get( new BigDecimal( denominator ) ) );
        }
        return get( new BigDecimal( numerator ) ).divide( new BigInteger( denominator ) );
      }
      else if( denominatorIsDecimal )
      {
        return get( new BigInteger( numerator ) ).divide( get( new BigDecimal( denominator ) ) );
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

  public BigInteger wholePart() {
    return _numerator.divide( _denominator );
  }

  public Rational fractionPart()
  {
    BigInteger remainder = _numerator.remainder( _denominator );
    if( remainder.signum() == 0 )
    {
      return ZERO;
    }
    return Rational.get( remainder, _denominator );
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

  public Rational add( int i )
  {
    return i == 0 ? this : add( get( i ) );
  }

  public Rational add( long l )
  {
    return l == 0 ? this : add( get( l ) );
  }

  public Rational add( float f )
  {
    return f == 0 ? this : add( get( f ) );
  }

  public Rational add( double d )
  {
    return d == 0 ? this : add( get( d ) );
  }

  public Rational add( BigInteger bg )
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

  public Rational add( BigDecimal bd )
  {
    return bd.signum() == 0 ? this : add( get( bd ) );
  }

  public Rational add( Rational rational )
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

  public Rational add( Number n )
  {
    if( n instanceof Rational )
    {
      return add( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return add( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return add( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return add( (double)n );
    }
    else if( n instanceof Float )
    {
      return add( (float)n );
    }
    else if( n instanceof Integer )
    {
      return add( (int)n );
    }
    else if( n instanceof Long )
    {
      return add( (long)n );
    }
    else
    {
      return add( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational subtract( int i )
  {
    return subtract( BigInteger.valueOf( i ) );
  }

  public Rational subtract( long l )
  {
    return subtract( BigInteger.valueOf( l ) );
  }

  public Rational subtract( float f )
  {
    return subtract( get( f ) );
  }

  public Rational subtract( double d )
  {
    return subtract( get( d ) );
  }

  public Rational subtract( BigInteger bi )
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

  public Rational subtract( BigDecimal bd )
  {
    return bd.signum() == 0 ? this : subtract( get( bd ) );
  }

  public Rational subtract( Rational rational )
  {
    if( rational.signum() == 0 )
    {
      return this;
    }
    if( signum() == 0 )
    {
      return rational.negate();
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

  public Rational subtract( Number n )
  {
    if( n instanceof Rational )
    {
      return subtract( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return subtract( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return subtract( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return subtract( (double)n );
    }
    else if( n instanceof Float )
    {
      return subtract( (float)n );
    }
    else if( n instanceof Integer )
    {
      return subtract( (int)n );
    }
    else if( n instanceof Long )
    {
      return subtract( (long)n );
    }
    else
    {
      return subtract( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational multiply( int i )
  {
    if( i == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return multiply( BigInteger.valueOf( i ) );
  }

  public Rational multiply( long l )
  {
    if( l == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return multiply( BigInteger.valueOf( l ) );
  }

  public Rational multiply( float f )
  {
    if( f == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return multiply( get( f ) );
  }

  public Rational multiply( double d )
  {
    if( d == 0 || signum() == 0 )
    {
      return ZERO;
    }
    return multiply( get( d ) );
  }

  public Rational multiply( BigInteger bi )
  {
    if( signum() == 0 || bi.signum() == 0 )
    {
      return ZERO;
    }
    return get( bi.multiply( _numerator ), _denominator );
  }

  public Rational multiply( BigDecimal bd )
  {
    if( signum() == 0 || bd.signum() == 0 )
    {
      return ZERO;
    }
    return multiply( get( bd ) );
  }

  public Rational multiply( Rational rational )
  {
    if( signum() == 0 || rational.signum() == 0 )
    {
      return ZERO;
    }
    return get( _numerator.multiply( rational._numerator ),
      _denominator.multiply( rational._denominator ) );
  }

  public Rational multiply( Number n )
  {
    if( n instanceof Rational )
    {
      return multiply( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return multiply( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return multiply( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return multiply( (double)n );
    }
    else if( n instanceof Float )
    {
      return multiply( (float)n );
    }
    else if( n instanceof Integer )
    {
      return multiply( (int)n );
    }
    else if( n instanceof Long )
    {
      return multiply( (long)n );
    }
    else
    {
      return multiply( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational divide( int i )
  {
    return divide( BigInteger.valueOf( i ) );
  }

  public Rational divide( long l )
  {
    return divide( BigInteger.valueOf( l ) );
  }

  public Rational divide( float f )
  {
    return divide( get( f ) );
  }

  public Rational divide( double d )
  {
    return divide( get( d ) );
  }

  public Rational divide( BigInteger bi )
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

  public Rational divide( BigDecimal bd )
  {
    if( bd.signum() == 0 )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    if( signum() == 0 )
    {
      return ZERO;
    }
    return divide( get( bd ) );
  }

  public Rational divide( Rational rational )
  {
    if( rational.equals( ZERO ) )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    if( signum() == 0 )
    {
      return ZERO;
    }
    return multiply( rational.invert() );
  }

  public Rational divide( Number n )
  {
    if( n instanceof Rational )
    {
      return divide( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return divide( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return divide( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return divide( (double)n );
    }
    else if( n instanceof Float )
    {
      return divide( (float)n );
    }
    else if( n instanceof Integer )
    {
      return divide( (int)n );
    }
    else if( n instanceof Long )
    {
      return divide( (long)n );
    }
    else
    {
      return divide( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational remainder( int i )
  {
    return remainder( BigInteger.valueOf( i ) );
  }

  public Rational remainder( long l )
  {
    return remainder( BigInteger.valueOf( l ) );
  }

  public Rational remainder( float f )
  {
    return remainder( get( f ) );
  }

  public Rational remainder( double d )
  {
    return remainder( get( d ) );
  }

  public Rational remainder( BigInteger bi )
  {
    if( bi.equals( BigInteger.ZERO ) )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    return remainder( get( bi ) );
  }

  public Rational remainder( BigDecimal bd )
  {
    if( bd.signum() == 0 )
    {
      throw new ArithmeticException( "Divide by zero" );
    }
    return remainder( get( bd ) );
  }

  public Rational remainder( Rational rational )
  {
    Rational quotient = divide( rational );
    return subtract( rational.multiply( quotient.toBigInteger() ) ).abs();
  }

  public Rational remainder( Number n )
  {
    if( n instanceof Rational )
    {
      return remainder( (Rational)n );
    }
    else if( n instanceof BigDecimal )
    {
      return remainder( (BigDecimal)n );
    }
    else if( n instanceof BigInteger )
    {
      return remainder( (BigInteger)n );
    }
    else if( n instanceof Double )
    {
      return remainder( (double)n );
    }
    else if( n instanceof Float )
    {
      return remainder( (float)n );
    }
    else if( n instanceof Integer )
    {
      return remainder( (int)n );
    }
    else if( n instanceof Long )
    {
      return remainder( (long)n );
    }
    else
    {
      return remainder( Rational.get( String.valueOf( n ) ) );
    }
  }

  public Rational negate()
  {
    return get( _numerator.negate(), _denominator );
  }

  public Rational invert()
  {
    return get( _denominator, _numerator );
  }

  public Rational abs()
  {
    return signum() >= 0 ? this : negate();
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
    return add( step );
  }

  @Override
  public Rational nextNthInSequence( Rational step, Void unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    return add( step.multiply( index ) );
  }

  @Override
  public Rational previousInSequence( Rational step, Void unit )
  {
    step = step == null ? Rational.ONE : step;
    return subtract( step );
  }

  @Override
  public Rational previousNthInSequence( Rational step, Void unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    return subtract( step.multiply( index ) );
  }

  @Override
  public int compareTo( Rational that )
  {
    if( that == null )
    {
      return -1;
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

  public int signum()
  {
    return _numerator.signum();
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
    if( whole.signum() == 0  )
    {
      return fractionPart().toFractionString();
    }
    return whole + " " + fractionPart().abs().toFractionString();
  }

  public String toDecimalString() {
    return toBigDecimal().toString();
  }

  public String toPlainDecimalString() {
    return toBigDecimal().toPlainString();
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