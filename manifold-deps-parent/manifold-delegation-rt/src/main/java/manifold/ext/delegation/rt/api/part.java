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
 * of a {@code part} class. The field's declaring class, called the linking class, delegates the implementation of some
 * or all of its interfaces to the field.
 * <p/>
 * If part classes are not used for linked fields, delegation is limited to call forwarding, which does not allow the linking
 * class to safely override linked interface methods. Referred to as <a href="https://web.media.mit.edu/~lieber/Lieberary/OOP/Delegation/Delegation.html">the Self problem</a>,
 * if the linking class overrides an interface method and the method is called from the linked class, since the receiver
 * of the call is the linked class it will not call the linking class's override. This breach in delegation compromises
 * the integrity of the interface composition model.
 * <p/>
 * TODO: link to an example
 * <p/>
 * The {@code @part} annotation solves the self problem by integrating with the compiler so that, when used as a link,
 * the part class's {@code this} references evaluate to the linking class's instance where needed. Similar to the sub/super
 * class relationship, this strategy integrates a class and its linked parts to form a loosely coupled, dynamically
 * configurable component.
 */
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.CLASS )
public @interface part
{
}
