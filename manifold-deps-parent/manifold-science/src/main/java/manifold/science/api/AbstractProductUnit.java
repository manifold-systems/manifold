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

import manifold.science.Area;
import manifold.science.LengthUnit;
import manifold.science.util.Rational;


import static manifold.science.util.Rational.ONE;

/**
 * Represents a binary product of unit types of measure such as {@link Area} which is the product of two {@link LengthUnit}s.
 * <p/>
 * @param <A> The unit type on the left hand side
 * @param <B> The unit type on the right hand side
 * @param <D> The {@link Dimension} type expressed using this binary product of unit types
 * @param <U> This type (recursive to enforce type-safety).
 */
public abstract class AbstractProductUnit<A extends Unit,
  B extends Unit,
  D extends Dimension<D>,
  U extends AbstractProductUnit<A, B, D, U>> extends AbstractBinaryUnit<A, B, D, U>
{

  protected AbstractProductUnit( A leftUnit, B rightUnit )
  {
    this( leftUnit, rightUnit, null, null, null );
  }

  protected AbstractProductUnit( A leftUnit, B rightUnit, Rational factor )
  {
    this( leftUnit, rightUnit, factor, null, null );
  }

  protected AbstractProductUnit( A leftUnit, B rightUnit, Rational factor, String name )
  {
    this( leftUnit, rightUnit, factor, name, null );
  }

  protected AbstractProductUnit( A leftUnit, B rightUnit, Rational factor, String name, String symbol )
  {
    super( leftUnit, rightUnit, factor, name, symbol );
  }

  public String getName()
  {
    String unitName = super.getName();
    return unitName == null
           ? getLeftUnit().getName() + " " + getRightUnit().getName()
           : unitName;
  }

  public String getSymbol()
  {
    String unitSymbol = super.getSymbol();
    return unitSymbol == null
           ? getLeftUnit().getSymbol() + "\u22C5" + getRightUnit().getSymbol()
           : unitSymbol;
  }

  public String getFullName()
  {
    return getLeftUnit().getFullName() + " " + getRightUnit().getFullName();
  }

  public String getFullSymbol()
  {
    return getLeftUnit().getFullSymbol() + "\u22C5" + getRightUnit().getFullSymbol();
  }

  public Rational toBaseUnits( Rational myUnits )
  {
    return (getLeftUnit().toBaseUnits( ONE ) * getRightUnit().toBaseUnits( ONE )) * myUnits * getFactor();
  }

  public Rational toNumber()
  {
    return getLeftUnit().toNumber() * getRightUnit().toNumber();
  }

  public B div( A a )
  {
    return getRightUnit();
  }
  // reifies to same type :(
  //abstract public A divide( B b )
}

