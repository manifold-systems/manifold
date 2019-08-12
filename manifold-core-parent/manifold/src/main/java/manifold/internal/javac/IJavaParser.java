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

package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.util.Pair;

/**
 */
public interface IJavaParser
{
  boolean parseText( String src, List<CompilationUnitTree> trees, Consumer<SourcePositions> sourcePositions, Consumer<DocTrees> docTrees, DiagnosticCollector<JavaFileObject> errorHandler );

  boolean parseType( String fqn, List<CompilationUnitTree> trees, DiagnosticCollector<JavaFileObject> errorHandler );

  InMemoryClassJavaFileObject compile( JavaFileObject jfo, String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler );

  Collection<InMemoryClassJavaFileObject> compile( Collection<JavaFileObject> jfo, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler );

  InMemoryClassJavaFileObject compile( String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler );

  Pair<JavaFileObject, String> findJavaSource( String fqn, DiagnosticListener<JavaFileObject> errorHandler );

  void clear();
}
