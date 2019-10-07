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

package manifold.ext.extensions.java.math.BigInteger;

import java.math.BigInteger;
import manifold.ext.api.Extension;
import manifold.ext.api.ComparableUsing;
import manifold.ext.api.This;

/**
 * Extends {@code BigInteger} with arithmetic operator overloads and relational overloads
 */
@Extension
public abstract class ManBigIntegerExt implements ComparableUsing<BigInteger>
{
  /** Supports unary prefix operator {@code -} */
  public static BigInteger unaryMinus( @This BigInteger thiz )
  {
    return thiz.negate();
  }

  /** Supports binary operator {@code +} */
  public static BigInteger plus( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.add( operand );
  }
  /** Supports binary operator {@code -} */
  public static BigInteger minus( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.subtract( operand );
  }
  /** Supports binary operator {@code *} */
  public static BigInteger times( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.multiply( operand );
  }
  /** Supports binary operator {@code /} */
  public static BigInteger div( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.divide( operand );
  }
  /** Supports binary operator {@code %} */
  public static BigInteger rem( @This BigInteger thiz, BigInteger operand )
  {
    return thiz.remainder( operand );
  }

  /** Implements structural interface {@link ComparableUsing} to support relational operators {@code == != > >= < <=} */
  public static boolean compareToUsing( @This BigInteger thiz, BigInteger that, Operator op )
  {
    return ComparableUsing.compareToUsing( (ComparableUsing<BigInteger>)thiz, that, op );
  }
}
