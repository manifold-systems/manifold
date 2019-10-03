

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

public abstract class NumberRange<E extends Number & Comparable<E>, ME extends NumberRange<E, ME>> extends AbstractIterableRange<E, E, Void, ME>
{
  @SuppressWarnings({"UnusedDeclaration"})
  public NumberRange( E left, E right, E step )
  {
    this( left, right, step, true, true, false );
  }

  public NumberRange( E left, E right, E step, boolean bLeftClosed, boolean bRightClosed, boolean bReverse )
  {
    super( left, right, step, null, bLeftClosed, bRightClosed, bReverse );
  }
}