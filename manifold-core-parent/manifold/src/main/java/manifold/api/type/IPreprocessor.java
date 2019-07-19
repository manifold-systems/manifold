/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.type;

import java.net.URI;

/**
 * A preprocessor gets a crack at modifying the Java source code before the Java parser sees it.  All preprocessing
 * happens directly as part of the Java compiler, as such it is fast and much less involved than conventional build
 * script oriented systems.
 */
public interface IPreprocessor
{
  /**
   * @return The preferred order for this preprocessor. If two or more processors have the same preference, there is no
   * guarantee which precedes the other. However preprocessors are guaranteed to be grouped in order: {@code First -> None -> Last}
   */
  Order getPreferredOrder();

  /**
   * Preprocess source code which is then handed off to the Java parser.
   * <p/>
   * Things to consider when implementing this method:
   * <lu>
   * <li> As a general rule a preprocessor should maintain line number ordering of the original file, otherwise tooling such
   * as debuggers will not align with the original source.</li>
   * <li> This method is performance sensitive, all source code is processed here. </li>
   * <li> If source is unchanged, return the same {@code source} parameter. </li>
   * <li> A preprocessor that must tokenize the source can use the Java scanner like this:<br>
   *     {@code Scanner scanner = ScannerFactory.instance(JavacPlugin.instance().getContext()).newScanner(input, true);}</li>
   * </lu>
   *
   * @param uri
   * @param source The Java source corresponding with a source file about to be compiled.
   * @return The processed source code.
   */
  CharSequence process( URI uri, CharSequence source );

  /** Used to specify the preferred order a preprocessor runs wrt others */
  enum Order
  {
    /** Indicates a preprocessor should run before others */
    First,

    /** Indicates a preprocessor should run after others */
    Last,

    /** Indicates the order is insignificant */
    None
  }
}
