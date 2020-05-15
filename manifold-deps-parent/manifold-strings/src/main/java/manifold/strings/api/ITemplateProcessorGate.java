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

package manifold.strings.api;

import manifold.api.DisableStringLiteralTemplates;

/**
 * An <a href="https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html">SPI</a> to gate template
 * processing. Implement this interface to prevent Java classes from string template processing. This is especially
 * useful for use-cases involving generated code you do not control.
 *
 * @see DisableStringLiteralTemplates
 */
@SuppressWarnings("unused")
public interface ITemplateProcessorGate
{
  /**
   * Return true if the {@code typeName} should be excluded from string template processing
   * @param typeName A fully qualified type name.
   * @return {@code true} to exclude {@code typeName} from string template processing, otherwise {@code false}.
   */
  boolean exclude( String typeName );
}
