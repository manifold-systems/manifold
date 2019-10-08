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

import java.math.BigDecimal;
import java.math.BigInteger;

public interface CoercionConstants
{
  RationalCoercion r = RationalCoercion.INSTANCE;
  BigDecimalCoercion bd = BigDecimalCoercion.INSTANCE;
  BigIntegerCoercion bi = BigIntegerCoercion.INSTANCE;

  class RationalCoercion
  {
    static final RationalCoercion INSTANCE = new RationalCoercion();

    public Rational postfixBind( String value ) {
      return Rational.get( value );
    }
    public Rational postfixBind( Integer value ) {
      return Rational.get( value );
    }
    public Rational postfixBind( Long value ) {
      return Rational.get( value );
    }
    public Rational postfixBind( Float value ) {
      return Rational.get( value );
    }
    public Rational postfixBind( Double value ) {
      return Rational.get( value );
    }
    public Rational postfixBind( BigInteger value ) {
      return Rational.get( value );
    }
    public Rational postfixBind( BigDecimal value ) {
      return Rational.get( value );
    }
  }

  class BigDecimalCoercion
  {
    static final BigDecimalCoercion INSTANCE = new BigDecimalCoercion();

    public BigDecimal postfixBind( String value ) {
      return new BigDecimal( value );
    }
    public BigDecimal postfixBind( Integer value ) {
      return BigDecimal.valueOf( value );
    }
    public BigDecimal postfixBind( Long value ) {
      return BigDecimal.valueOf( value );
    }
    public BigDecimal postfixBind( Float value ) {
      return BigDecimal.valueOf( value );
    }
    public BigDecimal postfixBind( Double value ) {
      return BigDecimal.valueOf( value );
    }
    public BigDecimal postfixBind( BigInteger value ) {
      return new BigDecimal( value );
    }
  }

  class BigIntegerCoercion
  {
    static final BigIntegerCoercion INSTANCE = new BigIntegerCoercion();

    public BigInteger postfixBind( String value ) {
      return new BigInteger( value );
    }
    public BigInteger postfixBind( Integer value ) {
      return BigInteger.valueOf( value );
    }
    public BigInteger postfixBind( Long value ) {
      return BigInteger.valueOf( value );
    }
  }
}
