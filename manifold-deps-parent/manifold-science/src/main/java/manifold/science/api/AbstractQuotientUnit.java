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


import static manifold.science.util.Rational.ONE;

/**
 * Represents a binary quotient of unit types of measure such as {@link VelocityUnit} which is the quotient of
 * {@link LengthUnit} and {@link TimeUnit}.
 * <p/>
 * @param <A> The unit type on the left hand side
 * @param <B> The unit type on the right hand side
 * @param <D> The {@link Dimension} type expressed using this binary unit type
 * @param <U> This type (recursive to enforce type-safety).
 */
public abstract class AbstractQuotientUnit<A extends Unit,
  B extends Unit,
  D extends Dimension<D>,
  U extends AbstractQuotientUnit<A, B, D, U>> extends AbstractBinaryUnit<A, B, D, U>
{

  protected AbstractQuotientUnit( A leftUnit, B rightUnit )
  {
    this( leftUnit, rightUnit, null, null, null );
  }

  protected AbstractQuotientUnit( A leftUnit, B rightUnit, Rational factor )
  {
    this( leftUnit, rightUnit, factor, null, null );
  }

  protected AbstractQuotientUnit( A leftUnit, B rightUnit, Rational factor, String name )
  {
    this( leftUnit, rightUnit, factor, name, null );
  }

  protected AbstractQuotientUnit( A leftUnit, B rightUnit, Rational factor, String name, String symbol )
  {
    super( leftUnit, rightUnit, factor, name, symbol );
  }

  public String getName()
  {
    return super.getName() == null
           ? getLeftUnit().getName() + "/" + getRightUnit().getName()
           : super.getName();
  }

  public String getSymbol()
  {
    return super.getSymbol() == null
           ? getLeftUnit().getSymbol() + "/" + getRightUnit().getSymbol()
           : super.getSymbol();
  }

  public String getFullName()
  {
    return getLeftUnit().getFullName() + "/" + getRightUnit().getFullName();
  }

  public String getFullSymbol()
  {
    return getLeftUnit().getFullSymbol() + "/" + getRightUnit().getFullSymbol();
  }

  public Rational toBaseUnits( Rational myUnits )
  {
    return (getLeftUnit().toBaseUnits( ONE ) / getRightUnit().toBaseUnits( ONE )) * myUnits * getFactor();
  }

  public Rational toNumber()
  {
    return getLeftUnit().toNumber() / getRightUnit().toNumber();
  }

  public A times( B a )
  {
    return getLeftUnit();
  }
}
