/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gain direct, type-safe access to otherwise inaccessible classes/methods/fields with @{@link Jailbreak}.
 * <p/>
 * Annotate the type on a variable, parameter, or new expression with @{@link Jailbreak} to avoid the drudgery
 * and vulnerability of Java reflection.
 * <p/>
 * See the <a href="http://manifold.systems/docs.html#type-safe-reflection">Type-safe Reflection</a> documentation for
 * more information.
 * <p/>
 * See also {@link manifold.ext.extensions.java.lang.Object.ManObjectExt#jailbreak(Object)}
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
public @interface Jailbreak
{
}
