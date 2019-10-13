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

package manifold.science.api;


import java.util.Objects;
import manifold.collections.api.range.Sequential;
import manifold.science.measures.Length;
import manifold.science.measures.Mass;
import manifold.science.measures.Velocity;
import manifold.science.util.Rational;

/**
 * This class serves as the base class for a measured quantity. See {@link Length},
 * {@link Velocity}, {@link Mass}, etc. for examples.
 * <p/>
 * Instances of this class store the value (or magnitude) of the measure in terms of base units. Thus all arithmetic on
 * measures are performed using Base units, which permits measures of differing input units to work in calculations. A
 * measure instance also maintains a "display unit", which is used for display purposes and for working with other
 * systems requiring specific units.
 * <p/>
 * @param <U> The unit corresponding with the type e.g., Length specifies LengthUnit.
 * @param <T> This type. Note this type is recursive to enforce type-safety, normally the complicated generics are not
 *           exposed to users of the library e.g., see the {@link Length} measure.
 */
public abstract class AbstractMeasure<U extends Unit<T, U>, T extends AbstractMeasure<U, T>> implements Dimension<T>, Sequential<T, Rational, U>
{
  /** The magnitude stored in Base units */
  private final Rational _value;

  /** The unit used to display the value of this instance */
  private final U _displayUnit;

  /**
   * @param value       The value (or magnitude) of this measure instance
   * @param unit        The unit corresponding to the provided {@code value}
   * @param displayUnit The unit in which to display this measure
   */
  public AbstractMeasure( Rational value, U unit, U displayUnit )
  {
    _displayUnit = displayUnit;
    _value = unit.toBaseUnits( value );
  }

  /**
   * The unit on which all instances of this type are based. For instance, a {@code Length} dimension might use Meters
   * as the base unit because it is the SI standard.
   */
  public abstract U getBaseUnit();

  /**
   * Creates a new instance using the specified parameters.
   */
  public abstract T make( Rational value, U unit, U displayUnit );

  /**
   * Creates a new instance using the specified parameters.
   */
  public abstract T make( Rational value, U unit );

  /**
   * Copies this instance with a new display unit.
   */
  public T copy( U dsiplayUnit )
  {
    return make( _value, getBaseUnit(), dsiplayUnit );
  }

  @Override
  public T copy( Rational value )
  {
    return make( value, getBaseUnit(), getDisplayUnit() );
  }

  /**
   * @return The value of this measure in Base units.
   */
  public Rational getValue()
  {
    return _value;
  }

  /**
   * @return The unit in which this measure displays.
   */
  public U getDisplayUnit()
  {
    return _displayUnit;
  }

  @Override
  public T fromNumber( Rational p0 )
  {
    return make( p0, _displayUnit );
  }

  public T fromBaseNumber( Rational p0 )
  {
    return make( p0, getBaseUnit(), _displayUnit );
  }

  /**
   * @return The magnitude of this measure in terms of Display units.
   */
  @Override
  public Rational toNumber()
  {
    return toNumber( _displayUnit );
  }

  /**
   * @return The magnitude of this measure in terms of Base units.
   */
  @Override
  public Rational toBaseNumber()
  {
    return _value;
  }

  /**
   * Copy this measure using the specified {@code displayUnit}.
   */
  public T to( U displayUnit )
  {
    return copy( displayUnit );
  }

  /**
   * Get the magnitude of this measure in terms of the specified {@code unit}.
   */
  public Rational toNumber( U unit )
  {
    //noinspection unchecked
    return unit.from( (T)this );
  }

  @Override
  public String toString()
  {
    return toNumber().toBigDecimal().stripTrailingZeros().toPlainString() + " " + getDisplayUnit().getSymbol();
  }

  /**
   * @return The measure as a mixed fraction such as {@code "2 1/8 kg"}
   */
  public String toMixedString()
  {
    return toNumber().toMixedString() + " " + getDisplayUnit().getSymbol();
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _value, _displayUnit );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( getClass() != o.getClass() )
    {
      return false;
    }
    AbstractMeasure<?, ?> that = (AbstractMeasure<?, ?>)o;
    return _value.equals( that._value ) &&
           _displayUnit.equals( that._displayUnit );
  }

  /**
   * Use {@link #compareTo(T)} to implement the {@code ==} operator as it does not take into account the
   * {@code _displayUnit} which is inconsequential wrt the measure.
   */
  @Override
  public EqualityMode equalityMode()
  {
    return EqualityMode.CompareTo;
  }

  @Override
  public int compareTo( T that )
  {
    return _value.compareTo( ((AbstractMeasure)that)._value );
  }

  @Override
  public T nextInSequence( Rational step, U unit )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getDisplayUnit() : unit;
    return fromBaseNumber( toBaseNumber() + (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) );
  }

  @Override
  public T nextNthInSequence( Rational step, U unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getDisplayUnit() : unit;
    return fromBaseNumber( toNumber() + (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) * index );
  }

  @Override
  public T previousInSequence( Rational step, U unit )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getDisplayUnit() : unit;
    return fromBaseNumber( toNumber() - (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) );
  }

  @Override
  public T previousNthInSequence( Rational step, U unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getDisplayUnit() : unit;
    return fromBaseNumber( toNumber() - (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) * index );
  }

  public T plus( T operand )
  {
    return copy( toBaseNumber().plus( operand.toBaseNumber() ) );
  }

  public T minus( T operand )
  {
    return copy( toBaseNumber().minus( operand.toBaseNumber() ) );
  }

  public Rational div( T operand )
  {
    return toBaseNumber() / operand.toBaseNumber();
  }

  public Rational rem( T operand )
  {
    return toBaseNumber() % operand.toBaseNumber();
  }
}
