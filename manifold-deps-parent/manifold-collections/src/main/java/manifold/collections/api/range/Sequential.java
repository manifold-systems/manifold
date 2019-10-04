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

package manifold.collections.api.range;

/**
 * Implement {@code Sequential} if the set of possible instances of your type are a <i>sequence</i>, whereby given an
 * arbitrary instance of the type and some <i>step</i> or increment value, the <i>next</i> instance can be computed.
 * @param <E> The implementing type e.g., Rational or Length (recursive type)
 * @param <S> The step type e.g., Rational
 * @param <U> The unit type of the step e.g., Void or LengthUnit
 */
public interface Sequential<E extends Sequential<E, S, U>, S, U> extends Comparable<E>
{
  /**
   * Given a {@code step} and {@code unit} produces the next instance in the sequence. For instance, given a Length
   * value of {@code 1 meter}, a step value of {@code 5}, and a unit of {@code Centimeter}, the next value in the
   * sequence is {@code 1.05 meters}.
   * @param step A value indicating the number of units separating this instance from the next one in the sequence.
   * @param unit Specifies the unit of the {@code step} for instance {@code inch} or {@code month}. Note if the unit
   *             type is {@link Void}, the unit value is insignificant and will have a null value.
   * @return The next instance separated by {@code step} {@code unit}s from this instance.
   */
  E nextInSequence( S step, U unit );

  /**
   * Given a {@code step}, {@code unit}, and {@code index} produce the next instance in the sequence. For instance,
   * given a Length value of {@code 1 meter}, a step value of {@code 5}, a unit of {@code Centimeter}, and an index of
   * {@code 2}, the next value in the sequence is {@code 1.10 meters}.
   * @param step A value indicating the number of units separating this instance from the next one in the sequence.
   * @param unit Specifies the unit of the {@code step} for instance {@code inch} or {@code month}. Note if the unit
   *             type is {@link Void}, the unit value is insignificant and will have a null value.
   * @param index A offset in terms of {@code step} and {@code unit}, typically {@code 1}.
   * @return The next instance separated by {@code index * step * unit} from this instance.
   */
  E nextNthInSequence( S step, U unit, int index );

  /**
   * Given a {@code step} and {@code unit} produces the previous instance in the sequence. For instance, given a Length
   * value of {@code 1 meter}, a step value of {@code 5}, and a unit of {@code Centimeter}, the previous value in the
   * sequence is {@code 0.95 meters}.
   * @param step A value indicating the number of units separating this instance from the previous one in the sequence.
   * @param unit Specifies the unit of the {@code step} for instance {@code inch} or {@code month}. Note if the unit
   *             type is {@link Void}, the unit value is insignificant and will have a null value.
   * @return The previous instance separated by {@code step} {@code unit}s from this instance.
   */
  E previousInSequence( S step, U unit );

  /**
   * Given a {@code step}, {@code unit}, and {@code index} produce the previous instance in the sequence. For instance,
   * given a Length value of {@code 1 meter}, a step value of {@code 5}, a unit of {@code Centimeter}, and an index of
   * {@code 2}, the previous value in the sequence is {@code 0.90 meters}.
   * @param step A value indicating the number of units separating this instance from the previous one in the sequence.
   * @param unit Specifies the unit of the {@code step} for instance {@code inch} or {@code month}. Note if the unit
   *             type is {@link Void}, the unit value is insignificant and will have a null value.
   * @param index A offset in terms of {@code step} and {@code unit}, typically {@code 1}.
   * @return The previous instance separated by {@code index * step * unit} from this instance.
   */
  E previousNthInSequence( S step, U unit, int index );
}
