/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.rt.api;

import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.HashSet;
import java.util.Set;

public interface TypeMap
{
  LocklessLazyVar<Set<TypeMap>> PROVIDERS =
    LocklessLazyVar.make( () -> {
      Set<TypeMap> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, TypeMap.class, TypeMap.class.getClassLoader() );
      return registered;
    } );

  static TypeMap findFirst()
  {
    return TypeMap.PROVIDERS.get().stream()
      .filter( tm -> tm.getPriority() == Priority.High )
      .findFirst().orElse(
        TypeMap.PROVIDERS.get().stream().filter( tm -> tm.getPriority() != Priority.High )
          .findFirst().orElseThrow( () -> new IllegalStateException( "Missing TypeMap" ) ) );
  }

  enum Priority {Low, High}

  Class<?> getType( RawElement column, int type );

  default Priority getPriority()
  {
    return Priority.High;
  }
}
