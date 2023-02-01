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

package manifold.ext.delegation.rt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use {@code @link} to incorporate interface composition & delegation in your project. Apply {@code @link} to:
 * <ul>
 * <li> delegate one or more interface implementations of the declaring class to a field that provides the implementations, and </li>
 * <li> if the field is assigned a {@link part} class, integrate the part to safely override linked interfaces (solves <i>the Self problem</i>) </li>
 * <li> safely share super interface implementations (solves <i>the Diamond problem</i>)</li>
 * </ul>
 * Many of the class's interfaces may be linked to a single field. A class may have many linked fields.
 * <p/>
 * todo: provide examples for each of the bullet points above
 */
@Target( ElementType.FIELD )
@Retention( RetentionPolicy.RUNTIME )
public @interface link
{
  /**
   * Specify interfaces to link. This value overrides the declared type of the field. If no interfaces are specified (default),
   * the linked interfaces are derived from the field's declared type.
   */
  Class<?>[] value() default {};

  /**
   * If true, indicates this link is shared where interface overlap exists with other links. Otherwise, overlapping interfaces
   * are not linked and the class must implement them directly, or it must be declared abstract. If two or more links attempt
   * to share the same interface, a compiler error results.
   */
  boolean share() default false;
}
