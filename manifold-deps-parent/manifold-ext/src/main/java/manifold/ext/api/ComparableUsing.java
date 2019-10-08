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

package manifold.ext.api;

/**
 * Implement this Comparable extension to enable relational operators directly on your type.  Normally you only need to
 * implement {@link Comparable#compareTo(T)}, the remaining method in this interface have default implementations
 * suitable for most use-cases. However, if necessary you can override behavior for individual relational operators by
 * overriding {@code compareToUsing()}.
 * <p/>
 * Note implementing this interface automatically overloads {@code ==} and {@code !=}.  The default behavior delegates
 * to {@code compareTo()}, but you can easily change the behavior by overriding {@code equalityMode()}, see
 * {@link EqualityMode} options.
 */
@Structural
public interface ComparableUsing<T> extends Comparable<T>
{
  /**
   * Relational operators to implement
   */
  enum Operator { GT, GE, LT, LE, EQ, NE }

  /**
   * The mode indicating the method used to implement {@code ==} and {@code !=} operators.
   */
  enum EqualityMode
  {
    /** Uses {@code #compareTo()} method (default) */
    CompareTo,

    /** Uses {@code equals()} method */
    Equals,

    /** Uses {@code identity} comparison, same as Java's {@code ==} behavior } */
    Identity
  }

  /**
   * Compare {@code this} to {@code that} using {@code op}.
   */
  default boolean compareToUsing( T that, Operator op )
  {
    switch( op )
    {
      case LT:
        return compareTo( that ) < 0;
      case LE:
        return compareTo( that ) <= 0;
      case GT:
        return compareTo( that ) > 0;
      case GE:
        return compareTo( that ) >= 0;

      case EQ:
      {
        switch( equalityMode() )
        {
          case CompareTo:
            return compareTo( that ) == 0;
          case Equals:
            return equals( that );
          case Identity:
            return this == that;
        }
      }

      case NE:
      {
        switch( equalityMode() )
        {
          case CompareTo:
            return compareTo( that ) != 0;
          case Equals:
            return !equals( that );
          case Identity:
            return this != that;
        }
      }

      default:
        throw new IllegalStateException();
    }
  }

  /** The method used to handle {@code ==} and {@code !=} */
  default EqualityMode equalityMode()
  {
    return EqualityMode.CompareTo;
  }
}
