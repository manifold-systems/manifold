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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare a structural interface using this annotation.
 * <p/>
 * See the <a href="https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural">Structural Interfaces</a>
 * documentation for more information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Structural
{
  /**
   * Optional.
   * <p/>
   * A factory class that produces a proxy for a structural interface.  The factory class must provide a public default
   * constructor and must implement {@link IProxyFactory}.
   * <p/>
   * Note, the factory class exists to address performance and to facilitate proxies where structural interface methods
   * are implemented indirectly as extension methods. In the latter case a factory class is required either here in the
   * &#64;Structural declaration or as a service. Note if there are more than one factory classes, they should be
   * provided as service implementations in the META-INF/services/manifold.ext.rt.api.IProxyFactory file, or listed in
   * your module's module-info.java file if you are using JDK 9+ with named modules.
   */
  Class factoryClass() default Void.class;
}
