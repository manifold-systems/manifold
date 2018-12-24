/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.api.type;

/**
 * Indicates the involvement of a {@link ITypeManifold} toward the completeness of projected source
 */
public enum ContributorKind
{
  /**
   * Contributes complete valid source and does not depend on contributions from other manifolds,
   * however other manifolds may augment the primary source.
   */
  Primary,

  /**
   * Cooperates with other manifolds to collectively provide complete valid source.
   */
  Partial,

  /**
   * Supplements the source produced from a Primary manifold or set of Partial manifolds. Any
   * number of Supplemental manifolds may contribute toward a single source.
   */
  Supplemental,

  /**
   * Does not contribute source.
   */
  None
}
