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
 * For internal use only.
 * <p/>
 * This annotation preserves declared state of a {@code @}{@link prop} field on the property's corresponding accessor
 * method[s]. In the case of property that must have a backing field, the {@link #flags} store the field's access so it
 * can be changed to {@code private} access in bytecode and then restored to the originally declared access during
 * compilation immediately after the declaring type's .class file is loaded. Similarly, when a property does not have a
 * backing field, {@link prop}, {@link get}, and {@link set} are used to recreate the erased symbol.
 */
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Retention( RetentionPolicy.RUNTIME )
public @interface propgen
{
  String name();
  long flags();
  prop[] prop() default {};
  get[] get() default {};
  set[] set() default {};
}
