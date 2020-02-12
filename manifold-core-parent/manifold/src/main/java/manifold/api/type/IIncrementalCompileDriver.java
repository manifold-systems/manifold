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

import java.io.File;
import java.util.Collection;

/**
 * Facilitates incremental compilation and hot swap debugging of Manifold resource files.
 * <p/>
 * The implementation of this interface is provided to Manifold during javac compilation.
 * It is provided by name (typically) using a generated, temporary Java source file annotated with
 * {@link IncrementalCompile}.  Typically an IDE hosting an incremental compiler using
 * Javac will keep track of the set of changed resource files needing compilation and generate
 * this temporary Java class. Thus an implementor of this interface acts as a mediator so that:
 * <ul>
 *   <li>the incremental compiler of the IDE can communicate the set of changed resource files to Manifold (via {@link #getChangedFiles()})</li>
 *   <li>Manifold can add the types associated with the resource files to javac's queue of types to be compiled </li>
 *   <li>Manifold can communicate back to the IDE the set of types via IjChangedResourceFiles#getTypesToFile()</li>
 * </ul>
 */
public interface IIncrementalCompileDriver
{
  /**
   * Is the compilation incremental, or is this a rebuild (full build)?
   * @return true if an incremental build, otherwise false indicating a rebuild.
   */
  boolean isIncremental();

  /**
   * Manifold's javac plugin calls this method after the ANALYZE phase of the class annotated with this method.  Typically
   * the class annotated with this method is temporary and generated on the fly and within the IDE hosting the compiler.
   * The IDE keeps track of resource files that have changed.  Returns all changed resources files (skips Java files);
   * Manifold will figure out whether or not each resource file maps to a Type Manifold and, if so, finds the type[s] produced from
   * the file. In turn the javac plugin associates types corresponding with the file via IjChangedResourceFiles#getTypesToFile().
   *
   * @return The resource files that have changed since the last make/build.
   */
  Collection<File> getChangedFiles();
}
