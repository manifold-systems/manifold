

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

public final class ComparableRange<E extends Comparable<E>> extends AbstractRange<E, ComparableRange<E>>
{
  public ComparableRange( E left, E right )
  {
    this( left, right, true, true, false );
  }

  public ComparableRange( E left, E right, boolean bLeftClosed, boolean bRightClosed, boolean bReverse )
  {
    super( left, right, bLeftClosed, bRightClosed, bReverse );
  }
}