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
import java.util.HashSet;
import java.util.Set;

/**
 * Notifies implementors immediately after a JDBC connection is established. Useful for testing, debugging, and connection
 * related environment setup.
 * <p/>
 * Note, implement {@link manifold.sql.rt.api.ConnectionProvider} to control the Connection itself.
 */
public interface ConnectionNotifier
{
  LocklessLazyVar<Set<ConnectionNotifier>> PROVIDERS =
    LocklessLazyVar.make( () -> {
      Set<ConnectionNotifier> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, ConnectionNotifier.class, ConnectionNotifier.class.getClassLoader() );
      return registered;
    } );

  /**
   * Invoked immediately after a JDBC connection is established.
   */
  void init( Connection c );
}
