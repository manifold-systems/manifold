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
 * Declares a read-only property, or modifies the getter of a read-write property . Adding {@link set} along with
 * {@link get} makes the property read-write; the same effect as {@link var}. Note {@link val} can also be used to
 * designate a read-only property.
 * <p/>
 * @see set
 * @see val
 * @see var
 */
@Target( {ElementType.FIELD} )
@Retention( RetentionPolicy.CLASS )
public @interface get
{
  /**
   * Use this argument to override the property's declared access.
   */
  PropOption[] value() default {};

  /**
   * Use this argument to specify annotations to apply to the property's generated getter methods.
   */
  any[] annos() default {};
}
