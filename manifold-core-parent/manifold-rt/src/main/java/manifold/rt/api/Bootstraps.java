/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.rt.api;

import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.*;

public class Bootstraps
{
  private static final LocklessLazyVar<Set<IBootstrap>> BOOTSTRAPS = LocklessLazyVar.make( () ->
    ServiceUtil.loadRegisteredServices(
      new LinkedHashSet<>(), IBootstrap.class, Bootstraps.class.getClassLoader() ) );

  public static Set<IBootstrap> get()
  {
    return BOOTSTRAPS.get();
  }
}