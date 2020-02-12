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

package manifold.api.util.cache;

import java.util.Set;
import java.util.function.Predicate;

/**
 */
public interface IFqnCache<T>
{
  T get( String fqn );

  FqnCacheNode getNode( String fqn );

  boolean contains( String fqn );

  void add( String fqn );

  void add( String fqn, T userData );

  void remove( String[] fqns );

  boolean remove( String fqn );

  void clear();

  Set<String> getFqns();

  /**
   * @param visitor returns whether or not to terminate visiting
   */
  boolean visitDepthFirst( Predicate<T> visitor );

  boolean visitNodeDepthFirst( Predicate<FqnCacheNode> visitor );
}
