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

import javax.tools.JavaFileObject;
import manifold.internal.javac.IssueReporter;

/**
 * A self-compiled type manifold provides its own Java bytecode for some or all of its types.
 * Thus, its implementation of {@link ITypeManifold#contribute} is typically a stub limited to
 * member declarations. So, instead of using javac to compile your source to disk, you use it
 * to parse your stub to allow other types to make references to your type. But the bytecode for
 * your type comes from your implementation of {@link #compile(String)}.
 */
public interface ISelfCompiled
{
  /**
   * Tests if a type {@code fqn} is self-compiled where subsequent calls to {@link #parse(String)}
   * and {@link #compile(String)} are necessary to compile the type to bytecode.
   * </p>
   * @param fqn The fully qualified name of the type.
   * @return {@code true} if the type corresponding with {@code fqn} is self-compiled.
   */
  default boolean isSelfCompile( String fqn )
  {
    return false;
  }

  /**
   * Signal this type manifold to fully parse the type corresponding with {@code fqn} and attach parse errors/warnings
   * to the source file, for example using {@link IssueReporter}&lt;{@link JavaFileObject}&gt;.
   * <p/>
   * For instance, the Gosu language manifold has its own bytecode compiler and handles this
   * call by fully parsing {@code fqn}. Note this call is made immediately before the Java
   * compiler's attribution phase, which makes it ok for member definitions and such to fully
   * resolve.
   *
   * @param fqn The fully qualified name of the type to parse.
   */
  default void parse( String fqn )
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Instructs a self-compiling type manifold to compile {@code fqn} and return resulting Java byte-code. Note this call
   * is only made if no errors were attached during {@link #parse(String)}.
   *
   * @param fqn The fully qualified name of the type to compile.
   */
  default byte[] compile( String fqn )
  {
    throw new UnsupportedOperationException();
  }
}
