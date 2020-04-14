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

import java.util.Collection;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IModule;
import manifold.api.host.IRuntimeManifoldHost;

/**
 * A {@link ITypeManifold} is a fundamental component of the Manifold API. Implementors of this interface
 * work together to dynamically provide the Java compiler with type information as it requests it.
 * <p/>
 * Its primary duties include:
 * <lu>
 *   <li>Define a domain of types via {@link #getTypeNames(String)}</li>
 *   <li>Resolve types in that domain via {@link #isType(String)}, {@link #isTopLevelType(String)}, and {@link #isPackage(String)}</li>
 *   <li>Contribute source toward a given type projection via {@link #contribute(JavaFileManager.Location, String, boolean, String, DiagnosticListener)}</li>
 * </lu>
 * <p/>
 * Separate instances of a given implementation of this interface exist per {@link manifold.api.host.IModule}.
 */
public interface ITypeManifold extends IFileConnected, ISelfCompiled
{
  /**
   *  System property to aid in debugging generated source.
   */
  String ARG_DUMP_SOURCE = "manifold.dump.source";

  /**
   * A module calls this method to determine whether or not to include this type manifold in its collection of type
   * manifolds.  Gives this type manifold an opportunity to opt out of inclusion based on the module. For instance,
   * if a module should only operate in a runtime environment, it should return false if the module's host is not
   * an instance of {@link IRuntimeManifoldHost}.
   * <p/>
   * Called after instantiation and, if returns true, before {@link #init(IModule)}.
   *
   * @param module The module asking for acceptance
   * @return {@code true} If this type manifold should be initialized and included in the module,
   * otherwise {@code false} to be discarded.
   */
  default boolean accept( IModule module )
  {
    return true;
  }

  /**
   * Initialize this type manifold.  Avoid defining types in the scope of this method.
   *
   * @param module The module to which this type manifold exclusively belongs
   */
  void init( IModule module );

  /**
   * The module to which this producer is scoped
   */
  IModule getModule();

  /**
   * What kind of source is produced?  Java?
   */
  ISourceKind getSourceKind();

  /**
   * How does this producer contribute toward the source file produced
   */
  ContributorKind getContributorKind();

  /**
   * Does this producer supply source for the specified fqn?
   */
  boolean isType( String fqn );

  boolean isTopLevelType( String fqn );

  /**
   * Verifies whether or not the specified package may be provided by this source producer
   */
  boolean isPackage( String pkg );

  /**
   * What kind of type corresponds with fqn?
   */
  ClassType getClassType( String fqn );

  /**
   * What is the package name for the specified fqn?
   */
  String getPackage( String fqn );

  /**
   * Contribute source corresponding with the fqn.
   */
  String contribute( JavaFileManager.Location location, String fqn, boolean genStubs, String existing, DiagnosticListener<JavaFileObject> errorHandler );

  Collection<String> getAllTypeNames();

  Collection<TypeName> getTypeNames( String namespace );

  List<IFile> findFilesForType( String fqn );

  /**
   * Clear all cached data
   */
  void clear();

  /**
   * Signals that normal javac compilation is complete with respect to the supplied Java source file list passed
   * to javac. Any "other" source files passed in (via other.source.files property using -Akey=value arg) are
   * about to be compiled. This method was added to facilitate a self-compiled type manifolds such as Gosu where
   * it can reliably depend on Java ClassSymbols to be complete from the JavacPlugin's javacTask. This in turn
   * enables the sharing of these symbols so the type manifold doesn't have to load a second javacTask and potentially
   * duplicate expensive work.
   */
  default void enterPostJavaCompilation()
  {
  }
}
