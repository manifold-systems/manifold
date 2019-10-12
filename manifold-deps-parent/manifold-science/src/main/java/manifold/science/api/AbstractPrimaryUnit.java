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
import manifold.science.util.Rational;

public abstract class AbstractPrimaryUnit<D extends Dimension<D>, U extends Unit<D, U>> implements Unit<D, U>
{
  private final Rational _baseFactor;
  private final String _name;
  private final String _symbol;

  protected AbstractPrimaryUnit( Rational baseFactor, String name, String symbol )
  {
    _baseFactor = baseFactor;
    _name = name;
    _symbol = symbol;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getSymbol()
  {
    return _symbol;
  }

  @Override
  public Rational toBaseUnits( Rational theseUnits )
  {
    return _baseFactor * theseUnits;
  }

  @Override
  public Rational from( D dim )
  {
    return dim.toBaseNumber() / _baseFactor;
  }

  @Override
  public final Rational toNumber()
  {
    return _baseFactor;
  }

  /**
   * Equality is based on {@code _baseFactor} alone, since two measures having the same factor of base unit are measures
   * of the same logical unit.
   */
  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !(o instanceof AbstractPrimaryUnit) )
    {
      return false;
    }
    AbstractPrimaryUnit<?, ?> that = (AbstractPrimaryUnit<?, ?>)o;
    return _baseFactor.equals( that._baseFactor );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _baseFactor );
  }
}
