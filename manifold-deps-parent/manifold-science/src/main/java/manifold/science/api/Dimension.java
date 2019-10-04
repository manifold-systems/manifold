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

import manifold.ext.api.IComparableWith;
import manifold.science.util.Rational;

public interface Dimension<S extends Dimension<S>> extends IComparableWith<S>
{
  /**
   * Given a value produce a copy of this dimension with the given value
   * @return
   */
  S copy( Rational value );

  /**
   * When adding two of the same dimension types, this method is called on each operand,
   * adds the numbers, and then calls fromNumber() for the result.
   *
   * @return the number of units for this dimension instance.
   */
  Rational toNumber();

  default Rational toBaseNumber()
  {
    return toNumber();
  }

  /**
   * Returns a separate instance of this type with the given number of units.
   * <p>
   * This method is called when performing default operations. For instance, when adding two
   * of the same dimension types, toNumber() is called on each operand, adds the numbers, and
   * then calls fromNumber() for the result.
   *
   * @return a separate instance of this type given the number of units.
   */
  S fromNumber( Rational value );

  /*
   * Default arithmetic behavior.
   */

  default S negate() {
    return copy( toBaseNumber().negate() );
  }

  default S add( S operand )
  {
    return copy( toBaseNumber().add( operand.toBaseNumber() ) );
  }
  default S subtract( S operand )
  {
    return copy( toBaseNumber().subtract( operand.toBaseNumber() ) );
  }
  default Rational divide( S operand )
  {
    return toBaseNumber() / operand.toBaseNumber();
  }
  default Rational remainder( S operand )
  {
    return toBaseNumber() % operand.toBaseNumber();
  }

  default S multiply( Number operand )
  {
    return copy( toBaseNumber() * operand );
  }
  default S divide( Number operand )
  {
    return copy( toBaseNumber() / operand );
  }
  default S remainder( Number operand )
  {
    return copy( toBaseNumber() % operand );
  }
}
