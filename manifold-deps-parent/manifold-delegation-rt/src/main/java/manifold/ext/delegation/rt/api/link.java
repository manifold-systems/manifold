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
 * Use {@code @link} to automatically transfer calls on unimplemented interface methods to fields in the same class.
 * <ul>
 * <li> Choose between call forwarding and true delegation with {@code @part} </li>
 * <li> Override linked interface methods, optionally using {@link part} classes (solves <a href="https://web.media.mit.edu/~lieber/Lieberary/OOP/Delegation/Delegation.html">the Self problem</a>) </li></li>
 * <li> Share super interface implementations (solves <a href="https://en.wikipedia.org/wiki/Multiple_inheritance#The_diamond_problem">the Diamond problem</a>)</li>
 * <li> Configure class implementation dynamically</li>
 * </ul>
 * Classes and links are many-to-many: Many of a class's interfaces may be linked to a single field. A single class may
 * have many linked fields.
 * <p/>
 * <b>Basic usage</b><br/>
 * <pre><code>
 * class MyClass implements MyInterface {
 *   &#64;link MyInterface myInterface; // transfers calls on MyInterface to myInterface
 *
 *   public MyClass(MyInterface myInterface) {
 *     this.myInterface = myInterface; // dynamically configure behavior
 *   }
 * }
 * </code></pre>
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
   * Where interface overlap exists with other links, this list of interfaces resolves which links to use. Otherwise,
   * overlapping interfaces are not linked and the class must implement them directly, or it must be declared abstract.
   * If two or more links declare to share the same interface, a compiler error results.
   */
  Class<?>[] share() default {};

  /**
   * If true, indicates this link is shared where interface overlap exists with other links. Similar to {@link #share},
   * but includes all interfaces from this link that overlap.
   */
  boolean shareAll() default false;
}
