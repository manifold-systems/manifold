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

import manifold.ext.rt.api.ComparableUsing;
import manifold.science.util.Rational;

import java.io.Serializable;

/**
 * {@code Dimension} is the basis for a physical dimension. It models a dimension has having a measure represented as a
 * {@link Rational} value and common arithmetic operations which also serve as operator overloads.
 * <p/>
 *
 * @param <S> Abstract dimension types are recursively defined to enforce type-safety
 */
public interface Dimension<S extends Dimension<S>> extends ComparableUsing<S>, Serializable
{
  /**
   * Given a value produce a copy of this dimension with the given value
   */
  S copy( Rational value );

  /**
   * @return the value for this dimension instance.
   */
  Rational toNumber();

  default Rational toBaseNumber()
  {
    return toNumber();
  }

  /**
   * @return a separate instance of this type with the given {@code value}.
   */
  S fromNumber( Rational value );


  /* Default arithmetic implementation */

  default S unaryMinus()
  {
    return copy( toBaseNumber().unaryMinus() );
  }

  default S plus( S operand )
  {
    return copy( toBaseNumber().plus( operand.toBaseNumber() ) );
  }

  default S minus( S operand )
  {
    return copy( toBaseNumber().minus( operand.toBaseNumber() ) );
  }

  default Rational div( S operand )
  {
    return toBaseNumber() / operand.toBaseNumber();
  }

  default Rational rem( S operand )
  {
    return toBaseNumber() % operand.toBaseNumber();
  }

  default S times( Number operand )
  {
    return copy( toBaseNumber() * operand );
  }

  default S div( Number operand )
  {
    return copy( toBaseNumber() / operand );
  }

  default S rem( Number operand )
  {
    return copy( toBaseNumber() % operand );
  }
}
