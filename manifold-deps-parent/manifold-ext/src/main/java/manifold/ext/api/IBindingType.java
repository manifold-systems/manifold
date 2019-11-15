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

package manifold.ext.api;

import javax.script.Bindings;

/**
 * {@code enum} types implement this so dynamic proxies can marshal calls to {@link Bindings}.
 */
public interface IBindingType
{
  /**
   * Produce a value suitable for a {@link Bindings} value e.g., a simple value like a string or
   * number or another Bindings or List of such values.
   */
  Object toBindingValue();
}
