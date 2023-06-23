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

package manifold.sql.rt.connection;

import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

public interface TestContextProvider
{
  LocklessLazyVar<Set<TestContextProvider>> PROVIDERS =
    LocklessLazyVar.make( () -> {
      Set<TestContextProvider> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, TestContextProvider.class, TestContextProvider.class.getClassLoader() );
      return registered;
    } );

  void init( Connection c );
}
