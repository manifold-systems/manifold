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

package manifold.science.extensions.java.math.BigDecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import manifold.ext.api.Extension;
import manifold.ext.api.ComparableUsing;
import manifold.ext.api.This;

/**
 * Extends {@code BigDecimal} with arithmetic and relational operator implementations
 */
@Extension
public abstract class ManBigDecimalExt implements ComparableUsing<BigDecimal>
{
  /**
   * Supports unary prefix operator {@code -}
   */
  public static BigDecimal unaryMinus( @This BigDecimal thiz )
  {
    return thiz.negate();
  }

  /**
   * Supports binary operator {@code +}
   */
  public static BigDecimal plus( @This BigDecimal thiz, BigDecimal operand )
  {
    return thiz.add( operand );
  }

  /**
   * Supports binary operator {@code -}
   */
  public static BigDecimal minus( @This BigDecimal thiz, BigDecimal operand )
  {
    return thiz.subtract( operand );
  }

  /**
   * Supports binary operator {@code *}
   */
  public static BigDecimal times( @This BigDecimal thiz, BigDecimal operand )
  {
    return thiz.multiply( operand );
  }

  /**
   * Supports binary operator {@code /}
   */
  public static BigDecimal div( @This BigDecimal thiz, BigDecimal operand )
  {
    return thiz.divide( operand, RoundingMode.HALF_EVEN );
  }

  /**
   * Supports binary operator {@code %}
   */
  public static BigDecimal rem( @This BigDecimal thiz, BigDecimal operand )
  {
    return thiz.remainder( operand );
  }

  /**
   * Implements structural interface {@link ComparableUsing} to support relational operators {@code == != > >= < <=}
   */
  public static boolean compareToUsing( @This BigDecimal thiz, BigDecimal that, Operator op )
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
