/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.ext.rt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method return type, parameter type, or a field type with @{@link Self} to achieve <i>Self</i>
 * type behavior.
 * <p/>
 * Note the {@link ElementType#METHOD} target is for <b>internal use only</b>.
 * This is necessary for generated code where even though the code applies the
 * {@link Self} annotation at the method return type position Java 8 misinterprets
 * it as a Method annotation, hence the METHOD target here. The METHOD target type
 * will be removed in a future release.
 * <p/>
 * See the <a href="http://manifold.systems/docs.html#the-self-type">Self Type</a> documentation
 * for more information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.METHOD})
public @interface Self
{
  /**
   * {@code value} is exclusive to <b>array extension methods</b>. Setting to {@code true} accesses the array's core
   * <i>component type</i> instead of the array type itself. E.g.,
   * <pre>
   *   public class MyArrayExtension {
   *   public static List<@Self(true) Object> toList(@This Object array) {
   *     return Arrays.asList((Object[])array);
   *   }
   * </pre>
   */
  boolean value() default false;
}
