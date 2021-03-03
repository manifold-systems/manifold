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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h2>For internal use only</h2>.
 * <p/>
 * Tags a property field as auto-generated during property inference.
 */
@Target( ElementType.FIELD )
@Retention( RetentionPolicy.CLASS )
public @interface auto
{
  /**
   * Applies to an existing field having the same name as an inferred property. Stores the declared
   * access privilege (public, protected, package, or private) of the original field. The default
   * value of -1 indicates the field did not exist prior to the inference.
   */
  int declaredAccess() default -1;
}
