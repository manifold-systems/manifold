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

package manifold.ext.rt.api;

/**
 * The {@link Structural#factoryClass}, if provided, must implement this interface.
 * @param <T> The type of the value the proxy will delegate to.
 * @param <I> The interface to proxy
 */
public interface IProxyFactory<T, I>
{
  /**
   * Create a proxy for the {@code iface} interface, delegating to {@code target}.
   * @param target The target value for the proxy
   * @param iface The interface to proxy
   * @return A proxy for the {@ocde iface} interface.
   */
  I proxy( T target, Class<I> iface );
}
