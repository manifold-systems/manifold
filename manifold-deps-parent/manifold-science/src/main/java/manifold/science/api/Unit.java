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

import manifold.science.util.Rational;

/**
 * Unit provides a base abstraction for postfix unit types such as length, time, mass, and velocity.
 * <p/>
 * @param <D> The {@link Dimension} type expressed using this unit type
 * @param <U> This type (recursive to enforce type-safety).
 */
public interface Unit<D extends Dimension<D>, U extends Unit<D, U>> extends Dimension<U>
{
  /**
   * @return The name of this unit, such as "Meter" or "Pound".
   */
  String getName();

  /**
   * @return The symbol for this unit. For example, SI units define "Meter" to have symbol "m".
   */
  String getSymbol();

  /**
   * @param theseUnits A magnitude of these units to convert to base units.
   * @return The {@code theseUnits} converted to the base units of this unit type.
   */
  Rational toBaseUnits( Rational theseUnits );

  Rational from( D dim );

  D makeDimension( Number amount );

  @Override
  default U copy( Rational value )
  {
    throw new IllegalStateException();
  }

  default String getFullName()
  {
    return getName();
  }

  default String getFullSymbol()
  {
    return getSymbol();
  }

  default D postfixBind( Number amount )
  {
    return makeDimension( amount );
  }

  @Override
  default U fromNumber( Rational n )
  {
    return null;
  }

  default int compareTo( U o )
  {
    return toNumber().compareTo( o.toNumber() );
  }
}
