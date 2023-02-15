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
 * When combined with the {@link link} annotation, a part class is a building block for interface composition & <i>true</i>
 * delegation. A part class may also be used as a regular class anywhere you would normally use a class.
 * <p/>
 * When used for delegation, one or more fields of a class are marked with {@code @link} where each is assigned an instance
 * of a {@code part} class. The field's enclosing class, called the linking class, delegates the implementation of linked
 * interfaces to a {@code part} class, where the linking class can override methods in the link. In turn, linked interfaces
 * invoked from the {@code part} class are also polymorphic with respect to the linking class. As with sub/super classes,
 * linked interface method calls are polymorphic both going it and out of a {@code part} class.
 */
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.CLASS )
public @interface part
{
}
