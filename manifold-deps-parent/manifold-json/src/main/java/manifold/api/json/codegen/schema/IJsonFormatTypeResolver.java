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

package manifold.api.json.codegen.schema;

import java.util.Set;
import manifold.ext.api.ICoercionProvider;

/**
 * Implement this interface as a <a href="https://docs.oracle.com/javase/tutorial/ext/basics/spi.html#register-service-providers">service provider</a>.
 * Provide a mapping from a JSON Schema {@code "format"} to a Java type. For instance, <p>
 * the {@code "date-time"} format maps to {@link java.time.LocalDateTime}
 * <p>
 * See {@link DefaultFormatResolver} to see how {@code "date-time"} and other formats are mapped to a set of Java types.
 * <p>
 * Register one or more of your {@code IJsonFormatTypeResolver} implementations in a file named:<b>
 * <pre>META-INF/services/manifold.api.json.codegen.schema.IJsonFormatTypeResolver</pre>
 * This file contains the fully qualified names of your implementations, one per line e.g.,
 * <pre>
 * com.example.FooFormatTypeResolver
 * com.example.BarFormatTypeResolver
 * </pre>
 */
public interface IJsonFormatTypeResolver extends ICoercionProvider
{
  /**
   * A list of one or more format names such as {@code "date-time", "date", "time"} to be referenced in Json Schema
   * {@code "format"} types.
   *
   * @return
   */
  Set<String> getFormats();

  /**
   * Given a JSON Schema {@code "format"} value such as {@code "date-time"} provide an {@link JsonFormatType}
   * to correspond with the format.
   * @param format The JSON Schema {@code "format"} value such as {@code "date-time"}.
   * @return An instance of {@link JsonFormatType} to correspond with the format or {@code null} if unresolved.
   */
  JsonFormatType resolveType( String format );
}
