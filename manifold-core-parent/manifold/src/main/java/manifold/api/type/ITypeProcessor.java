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

package manifold.api.type;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import manifold.internal.javac.IssueReporter;
import manifold.internal.javac.TypeProcessor;

/**
 * Implementors of this interface can examine and rewrite the Java AST during compilation.
 */
public interface ITypeProcessor
{
  /**
   * A typical implementation creates a {@link com.sun.tools.javac.tree.TreeTranslator} and
   * visits the tree in context e.g.,
   * <pre>
   *   TreeTranslator visitor = new ExtensionTransformer( this, typeProcessor );
   *   typeProcessor.getTree().accept( visitor );
   * </pre>
   */
  void process( TypeElement fqn, TypeProcessor typeProcessor, IssueReporter<JavaFileObject> issueReporter );

  //boolean filterError( TypeProcessor typeProcessor, Diagnostic diagnostic );
}
