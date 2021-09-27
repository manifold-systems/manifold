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

import java.lang.reflect.Type;

/**
 * Implement this interface to handle coercions during dynamic proxy invocation, such as with JSON Schema marshalling.
 * Note if you intend to implement JSON coercions you should instead implement {@code IJsonFormatTypeCoercer}.
 * <p>
 * See {@code IJsonFormatTypeCoercer}
 */
public interface ICoercionProvider
{
  /**
   * Coerce {@code value} to {@code type}.  Return {@link ICallHandler#UNHANDLED} if not coerced.
   * It is imperative that this method coerces only between the JSON types and Java types it explicitly
   * handles otherwise you may inadvertently perform a coercion that is incorrect that is better handled
   * by another coercion provider.
   * @param value A value to coerce, typically a String.
   * @param type The type to convert to.
   * @return A value of the specified {@code type} or {@link ICallHandler#UNHANDLED} if this provider does not handle
   * the coercion.
   */
  Object coerce( Object value, Type type );

  /**
   * Coerce {@code value} parameter to a value suitable for a {@link manifold.rt.api.Bindings} e.g., a JSON bindings.
   * It is imperative that this method coerces only between the JSON types and Java types it explicitly
   * handles otherwise you may inadvertently perform a coercion that is incorrect that is better handled
   * by another coercion provider.
   * @param value A value to coerce.
   * @return A value suitable for a {@link manifold.rt.api.Bindings} or {@link ICallHandler#UNHANDLED}
   * if this provider does not handle the coercion.  For example, the coerced value is typically a String when dealing
   * with JSON Schema formats such as {@code "date-time"} where the value persists in the bindings as a String, but
   * surfaces as a {@link java.time.LocalDateTime} in the Java API.
   */
  Object toBindingValue( Object value );
}
