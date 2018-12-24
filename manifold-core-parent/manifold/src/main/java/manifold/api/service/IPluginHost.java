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

package manifold.api.service;

import java.util.Collections;
import java.util.List;

/**
 * This simple interface provides the core foundation for component architecture.
 */
public interface IPluginHost
{
  /**
   * Provides an implementation of a specified interface.
   *
   * @return The implementation[s] of the interface or null if unsupported.
   */
  default <T> List<T> getInterface( Class<T> apiInterface )
  {
    return Collections.emptyList();
  }
}
