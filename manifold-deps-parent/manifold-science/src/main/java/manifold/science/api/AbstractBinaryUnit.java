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

import manifold.science.LengthUnit;
import manifold.science.TimeUnit;
import manifold.science.VelocityUnit;
import manifold.science.util.Rational;

/**
 * Represents a binary unit of measure such as {@link VelocityUnit} which is the quotient of {@link LengthUnit} and
 * {@link TimeUnit}.
 * <p/>
 * @param <A> The unit type on the left hand side
 * @param <B> The unit type on the right hand side
 * @param <D> The {@link Dimension} type expressed using this binary unit type
 * @param <U> This type (recursive to enforce type-safety).
 */
public abstract class AbstractBinaryUnit<A extends Unit,
  B extends Unit,
  D extends Dimension<D>,
  U extends AbstractBinaryUnit<A, B, D, U>> implements Unit<D, U>
{
  private final A _leftUnit;
  private final B _rightUnit;
  private final Rational _factor;
  private final String _name;
  private final String _symbol;

  protected AbstractBinaryUnit( A leftUnit, B rightUnit )
  {
    this( leftUnit, rightUnit, null, null, null );
  }

  protected AbstractBinaryUnit( A leftUnit, B rightUnit, Rational factor )
  {
    this( leftUnit, rightUnit, factor, null, null );
  }

  protected AbstractBinaryUnit( A leftUnit, B rightUnit, Rational factor, String name )
  {
    this( leftUnit, rightUnit, factor, name, null );
  }

  protected AbstractBinaryUnit( A leftUnit, B rightUnit, Rational factor, String name, String symbol )
  {
    _leftUnit = leftUnit;
    _rightUnit = rightUnit;
    _factor = factor == null ? Rational.ONE : factor;
    _name = name;
    _symbol = symbol;
  }

  protected A getLeftUnit()
  {
    return _leftUnit;
  }

  protected B getRightUnit()
  {
    return _rightUnit;
  }

  public Rational getFactor()
  {
    return _factor;
  }

  public String getName()
  {
    return _name;
  }

  public String getSymbol()
  {
    return _symbol;
  }

  @Override
  public Rational from( D r )
  {
    return r.toBaseNumber() / toBaseUnits( Rational.ONE );
  }

  @Override
  public String toString()
  {
    return getName();
  }

  @Override
  public int hashCode()
  {
    return 31 * (31 * _leftUnit.hashCode() + _rightUnit.hashCode()) + _factor.hashCode();
  }

  @Override
  public boolean equals( Object obj )
  {
    if( obj.getClass() != getClass() )
    {
      return false;
    }

    AbstractBinaryUnit that = (AbstractBinaryUnit)obj;

    return _leftUnit == that.getLeftUnit()
           && _rightUnit == that.getRightUnit()
           && _factor == that._factor;
    // note we don't want name here since we look up based on just left & right units
    // i.e., we don't want to have to name units all the time, just look them up by left & right units
    // && _name == that._name
  }
}
