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

package manifold.ext.props.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a field as having a property getter method. Specifying {@code @get} without a {@code @set} indicates the
 * property is <i>read-only</i>, thus you must specify {@code @set} for the field to be writable, or use {@code @prop}.
 * Note, use of {@code @prop} is optional when specifying {@code @get}.
 *
 * @see prop
 */
@Target( {ElementType.FIELD} )
@Retention( RetentionPolicy.CLASS )
public @interface get
{
  PropOption[] value() default {};
}
