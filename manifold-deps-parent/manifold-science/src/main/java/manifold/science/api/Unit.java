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

public interface Unit<D extends Dimension<D>, U extends Unit<D, U>> extends Dimension<U>
{
  String getUnitName();
  String getUnitSymbol();
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
    return getUnitName();
  }

  default String getFullSymbol()
  {
    return getUnitSymbol();
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
