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

import java.sql.Connection;
import java.util.*;
import java.util.function.Supplier;

public interface CrudProvider
{
  LocklessLazyVar<Set<CrudProvider>> PROVIDERS =
    LocklessLazyVar.make( () -> {
      Set<CrudProvider> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, CrudProvider.class, CrudProvider.class.getClassLoader() );
      return registered;
    } );

  LocklessLazyVar<CrudProvider> BY_PRIORITY =
    LocklessLazyVar.make( () ->
      PROVIDERS.get().stream().max( Comparator.comparingInt( CrudProvider::getPriority ) )
        .orElseThrow( () -> new IllegalStateException() ) );

  static CrudProvider instance()
  {
    return BY_PRIORITY.get();
  }


  <T extends TableRow> void create( Connection c, UpdateContext<T> ctx );
  <T extends TableRow> T read( QueryContext<T> ctx );
  <T extends TableRow> void update( Connection c, UpdateContext<T> ctx );
  <T extends TableRow> void delete( Connection c, UpdateContext<T> ctx );

  /**
   * Greater = higher priority. Higher priority overrides lower. Default implementations are lowest priority. They can be
   * overridden.
   */
  default int getPriority()
  {
    return Integer.MIN_VALUE;
  }
}
