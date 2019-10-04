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
import manifold.science.util.Rational;

public abstract class AbstractMeasure<U extends Unit<T, U>, T extends AbstractMeasure<U, T>> implements Dimension<T>, Sequential<T, Rational, U>
{
  private final Rational _value;
  private final U _dipslayUnit;
  private final U _baseUnit;

  public AbstractMeasure( Rational value, U unit, U displayUnit, U baseUnit )
  {
    _value = unit.toBaseUnits( value );
    _dipslayUnit = displayUnit;
    _baseUnit = baseUnit;
  }

  public abstract T make( Rational value, U unit, U displayUnit );
  public abstract T make( Rational value, U unit );

  public T copy( U unit )
  {
    return make( _value, _baseUnit, unit );
  }

  @Override
  public T copy( Rational value )
  {
    return make( value, getBaseUnit(), getUnit() );
  }

  public Rational getValue()
  {
    return _value;
  }

  public U getUnit()
  {
    return _dipslayUnit;
  }

  public U getBaseUnit()
  {
    return _baseUnit;
  }

  @Override
  public T fromNumber( Rational p0 )
  {
    return make( p0, _dipslayUnit );
  }

  public T fromBaseNumber( Rational p0 )
  {
    return make( p0, _baseUnit, _dipslayUnit );
  }

  @Override
  public Rational toNumber()
  {
    return toNumber( _dipslayUnit );
  }

  @Override
  public Rational toBaseNumber()
  {
    return _value;
  }

  public T to( U unit )
  {
    return copy( unit );
  }

  public Rational toNumber( U unit )
  {
    return unit.from( (T)this );
  }

  @Override
  public String toString()
  {
    return toNumber( getUnit() )
             .toBigDecimal().stripTrailingZeros().toPlainString() + " " + getUnit().getUnitSymbol();
  }

  @Override
  public int hashCode()
  {
    return 31 * _value.intValue() + _baseUnit.hashCode();
  }

  @Override
  public boolean equals( Object o )
  {
    if( o.getClass() != getClass() )
    {
      return false;
    }
    AbstractMeasure that = (AbstractMeasure)o;
    return Objects.equals( _baseUnit, that._baseUnit ) &&
           Objects.equals( _value, that._value );
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
    unit = unit == null ? getUnit() : unit;
    return fromBaseNumber( toBaseNumber() + (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) );
  }

  @Override
  public T nextNthInSequence( Rational step, U unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getUnit() : unit;
    return fromBaseNumber( toNumber() + (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) * index );
  }

  @Override
  public T previousInSequence( Rational step, U unit )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getUnit() : unit;
    return fromBaseNumber( toNumber() - (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) );
  }

  @Override
  public T previousNthInSequence( Rational step, U unit, int index )
  {
    step = step == null ? Rational.ONE : step;
    unit = unit == null ? getUnit() : unit;
    return fromBaseNumber( toNumber() - (unit.toBaseUnits( step ) - unit.toBaseUnits( Rational.ZERO )) * index );
  }

  public T add( T operand )
  {
    return copy( toBaseNumber().add( operand.toBaseNumber() ) );
  }
  public T subtract( T operand )
  {
    return copy( toBaseNumber().subtract( operand.toBaseNumber() ) );
  }
  public Rational divide( T operand )
  {
    return toBaseNumber() / operand.toBaseNumber();
  }
  public Rational remainder( T operand )
  {
    return toBaseNumber() % operand.toBaseNumber();
  }
}
