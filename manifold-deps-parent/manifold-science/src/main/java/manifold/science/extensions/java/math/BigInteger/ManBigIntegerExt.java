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

package manifold.science.extensions.java.math.BigInteger;

import java.math.BigInteger;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.ComparableUsing;
import manifold.ext.rt.api.This;

/**
 * Extends {@code BigInteger} with arithmetic and relational operator implementations
 */
@Extension
public abstract class ManBigIntegerExt implements ComparableUsing<BigInteger>
{
  /**
   * Supports unary prefix operator {@code -}
   */
  public static BigInteger unaryMinus( @This BigInteger thiz )
  {
    return thiz.negate();
  }

  /**
   * Supports unary increment operator {@code ++}
   */
  public static BigInteger inc( @This BigInteger thiz )
  {
    return thiz.add( BigInteger.ONE );
  }

  /**
   * Supports unary decrement operator {@code --}
   */
  public static BigInteger dec( @This BigInteger thiz )
  {
    return thiz.subtract( BigInteger.ONE );
  }

  /**
   * Supports binary operator {@code +}
   */
  public static BigInteger plus( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.add( operand );
  }

  /**
   * Supports binary operator {@code -}
   */
  public static BigInteger minus( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.subtract( operand );
  }

  /**
   * Supports binary operator {@code *}
   */
  public static BigInteger times( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.multiply( operand );
  }

  /**
   * Supports binary operator {@code /}
   */
  public static BigInteger div( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.divide( operand );
  }

  /**
   * Supports binary operator {@code %}
   */
  public static BigInteger rem( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.remainder( operand );
  }

  /**
   * Implements structural interface {@link ComparableUsing} to support relational operators {@code == != > >= < <=}
   */
  public static boolean compareToUsing( @This BigInteger thiz, BigInteger that, Operator op )
  {
    switch( op )
    {
      case LT:
        return thiz.compareTo( that ) < 0;
      case LE:
        return thiz.compareTo( that ) <= 0;
      case GT:
        return thiz.compareTo( that ) > 0;
      case GE:
        return thiz.compareTo( that ) >= 0;
      case EQ:
        return thiz.compareTo( that ) == 0;
      case NE:
        return thiz.compareTo( that ) != 0;

      default:
        throw new IllegalStateException();
    }
  }
}
