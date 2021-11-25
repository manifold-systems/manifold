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

package manifold.preprocessor.api;

import manifold.api.fs.IFile;
import manifold.preprocessor.definitions.Definitions;

/**
 * Implement this service provider interface to augment the preprocessor's symbol set with custom symbols.
 * See library, manifold-preprocessor-android-syms, for an example.
 */
public interface SymbolProvider
{
  /**
   * Returns true if the symbol exists in this provider.
   *
   * @param rootDefinitions The Definitions
   * @param sourceFile The path to the source file in context.
   * @param def The name of the preprocessor definition.
   * @return true iff the symbol exists in this provider.
   */
  boolean isDefined( Definitions rootDefinitions, IFile sourceFile, String def );

  /**
   * Returns the string representation of the value of {@code def} if it exists in this provider.
   *
   * @param rootDefinitions
   * @param sourceFile The path to the source file in context.
   * @param def The name of the preprocessor definition.
   * @return The string representation of the value of {@code def} or null if it does not exist in thie provider.
   */
  String getValue( Definitions rootDefinitions, IFile sourceFile, String def );
}
