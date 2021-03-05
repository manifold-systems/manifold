/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.props.rt.api;

import manifold.rt.api.anno.any;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a write-only property, or modifies the setter of a read-write property . Adding {@link get} along with
 * {@link set} makes the property read-write; the same effect as {@link var}.
 * <p/>
 * <pre><code>
 * {@literal @}var @set(Protected) String name; // a read-write property with public getter access and protected setter access
 * </code></pre>
 * @see set
 * @see val
 * @see var
 */
@Target( {ElementType.FIELD} )
@Retention( RetentionPolicy.CLASS )
public @interface set
{
  /**
   * Use this argument to override the property's declared access.
   */
  PropOption[] value() default {};

  /**
   * Use this argument to specify annotations to apply to the property's generated setter method.
   */
  any[] annos() default {};

  /**
   * Use this argument to specify annotations to apply to the property's generated setter parameter.
   */
  any[] param() default {};
}
