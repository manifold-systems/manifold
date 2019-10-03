

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

package manifold.science.api.range;

import java.util.Iterator;

public interface IterableRange<E extends Comparable<E>, S, U, ME extends IterableRange<E, S, U, ME>> extends Iterable<E>, Range<E, ME>
{
  /**
   * @return An iterator that visits the elements in this range in order, from left to right.
   *   Returns null if this range does not support iteration.
   *
   * @see #iterateFromLeft()
   * @see #iterateFromRight()
   */
  Iterator<E> iterator();

  /**
   * @return An iterator that visits the elements in this range in order, from left to right.
   *   Returns null if this range does not support iteration.
   *
   * @see #iterator()
   * @see #iterateFromRight()
   */
  Iterator<E> iterateFromLeft();

  /**
   * @return An iterator that visits the elements in this range in reverse order, from right to left.
   *   Returns null if this range does not support iteration.
   *
   * @see #iterator()
   * @see #iterateFromLeft()
   */
  Iterator<E> iterateFromRight();

  /**
   * @return The step (or increment) by which this range visits elements in its set. Returns null
   *   if this range cannot iterate its elements.
   * <p>
   * For instance, if the range is a set of decimal values, say [1..10], the step might be a decimal
   * increment, say 0.25. Similarly, if the range is simply a set of integers the step might also be
   * an integer value, typically 1. Considering a date range, say [4/5/10..5/20/10], the step could
   * be expressed in terms of a unit of time e.g., 10 seconds, 1 minute, 2 weeks, etc.
   * <p>
   * Note if non-null, the step is a <i>positive</i> (or absolute) increment. To iterate the range
   * in reverse order use iterateFromRight().
   */
  S getStep();

  ME step( S s );

  U getUnit();
  ME unit( U u );

  /**
   * @param iStepIndex The index of the step from the left endpoint
   * @return The nth step from the left endpoint. Returns null if iStepIndex is out of bounds.
   * @thows IllegalArgumentException if iStepIndex is < 0
   */
  E getFromLeft( int iStepIndex );

  /**
   * @param iStepIndex The index of the step from the right endpoint
   * @return The nth step from the right endpoint. Returns null if iStepIndex is out of bounds.
   * @thows IllegalArgumentException if iStepIndex is < 0
   */
  E getFromRight( int iStepIndex );
}