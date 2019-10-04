

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
 * A range of {@link Comparable} elements defined by two endpoints.
 *
 * @param <E>  The type of elements in the range, must implement {@link Comparable}
 * @param <ME> The range type (recursive)
 */
public interface Range<E extends Comparable<E>, ME extends Range<E, ME>>
{
  /**
   * @return The left endpoint of this range where the left <= right
   */
  E getLeftEndpoint();

  /**
   * @return The right endpoint of this range where the left <= right
   */
  E getRightEndpoint();

  /**
   * @return True if this range <i>includes</i> the left endpoint.
   */
  boolean isLeftClosed();

  /**
   * @return True if this range <i>includes</i> the right endpoint.
   */
  boolean isRightClosed();

  /**
   * @param elem An element to test
   *
   * @return True if elem is a proper element in the set of elements defining this range.
   */
  boolean contains( E elem );

  /**
   * @param range An range to test for containment
   *
   * @return True if range's endpoints are proper elements in the set of elements defining this range.
   */
  boolean contains( ME range );

  /**
   * @return True if this range iterates from the right by default e.g.,
   * if the range is specified in reverse order: 10..1, a reverse range results
   */
  boolean isReversed();
}
