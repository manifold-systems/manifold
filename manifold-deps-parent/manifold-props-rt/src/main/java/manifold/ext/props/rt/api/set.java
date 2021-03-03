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
 * Designates a field declaration as a write-only property. Adding @{@link get} along with @{@link set} makes the
 * property read-write; the same effect as @{@link var}.
 * <p/>
 * Note, it is recommended to use {@link get} and {@link set} exclusively as a means to override modifiers and
 * annotations on {@link var}, <i>or</i> as an alternative to using {@link var} and {@link val}.
 *
 * @see get
 * @see val
 * @see var
 */
@Target( {ElementType.FIELD} )
@Retention( RetentionPolicy.CLASS )
public @interface set
{
  PropOption[] value() default {};
  any[] annos() default {};
  any[] param() default {};
}
