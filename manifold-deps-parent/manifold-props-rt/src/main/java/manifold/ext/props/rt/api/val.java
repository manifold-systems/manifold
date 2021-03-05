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
 * Designates a field declaration as a read-only property.
 * <p/>
 * <pre><code>
 * {@literal @}val String name; // a public read-only property
 * </code></pre>
 * You use the property directly as declared:
 * <p/>
 * <pre><code>
 * String theName = foo.name; // compiles as a call to foo.getName()
 * </code></pre>
 * When used in an interface the property is public and abstract, a public getter is generated. If the property is
 * initialized, the getter is {@code default} and returns the initialized value.
 * <p/>
 * When used in a class the property is public by default, a private final backing field is generated having the same
 * name, and a public getter is generated returning the field. The property must be initialized either in its
 * declaration or in the constructor(s).
 * <p/>
 * A property can be declared with {@code static}. When used in a class, the same rules apply as non-static. Static
 * interface properties, however, must always be calculated -- getter/setter methods must be provided.
 * <p/>
 * Note, the {@code final} and {@code abstract} modifiers can be used in a property declaration; they apply to the
 * getter/setter accessor methods. Thus, if there are user-defined getter/setter methods corresponding with the
 * property, they must reflect the modifiers from the property.
 * <p/>
 * @see var
 * @see get
 * @see set
 */
@Target( {ElementType.FIELD} )
@Retention( RetentionPolicy.CLASS )
public @interface val
{
  /**
   * Access options to override the property field's declared access. Note, favor Java modifiers for properties over
   * PropOption annotation arguments. As such, the only necessary option is PropOption.Package to override the default
   * public nature of properties. Otherwise, you don't need to use this argument.
   */
  PropOption[] value() default {};

  /**
   * Use this argument to specify annotations to apply to the val's generated getter method.
   */
  any[] annos() default {};
}
