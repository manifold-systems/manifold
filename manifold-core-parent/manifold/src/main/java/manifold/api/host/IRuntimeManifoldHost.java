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

package manifold.api.host;

import java.io.File;
import java.util.List;
import manifold.util.NecessaryEvilUtil;

/**
 * A Manifold host exclusive to the runtime environment.  Responsible for
 * dynamic loading of Manifold types via ClassLoader integration.
 */
public interface IRuntimeManifoldHost extends IManifoldHost
{
  /**
   * Is Manifold bootstrapped?
   */
  boolean isBootstrapped();

  /**
   * Measures to be taken before {@link #bootstrap(List, List)} is invoked.
   */
  default void preBootstrap()
  {
    // reflectively make modules accessible such as java.base and jdk.compiler
    NecessaryEvilUtil.bypassJava9Security();
  }

  /**
   * Bootstrap Manifold before application code executes
   */
  void bootstrap( List<File> sourcepath, List<File> classpath );
}
