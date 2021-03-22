/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.science.measures;

import manifold.science.util.Rational;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface DimensionlessUnit
{
  Rational getAmount();

  String getUnitName();

  String getUnitSymbol();

  default Rational postfixBind( String value )
  {
    return getAmount() * Rational.get( value );
  }

  default Rational postfixBind( Integer value )
  {
    return getAmount() * value;
  }

  default Rational postfixBind( Long value )
  {
    return getAmount() * value;
  }

  default Rational postfixBind( Float value )
  {
    return getAmount() * value;
  }

  default Rational postfixBind( Double value )
  {
    return getAmount() * value;
  }

  default Rational postfixBind( BigInteger value )
  {
    return getAmount() * value;
  }

  default Rational postfixBind( BigDecimal value )
  {
    return getAmount() * value;
  }

  default Rational postfixBind( Rational value )
  {
    return getAmount() * value;
  }
}
