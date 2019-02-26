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

package manifold.api.host;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.cache.PathCache;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.util.ServiceUtil;

/**
 * Java projects are typically organized according to a hierarchy of modules where each module defines
 * packages of Java classes and resources and other modules on which it depends. {@link IModule} abstracts
 * this concept as a set of paths defining source, class, and output locations, a list of dependencies on
 * other modules, and the set of {@link ITypeManifold}s managed by the module.
 * <p/>
 * The use of modules for the purposes of the Manifold API is mostly applicable to IDE integration such
 * as with the <a href="http://manifold.systems/docs.html#working-with-intellij">IntelliJ plugin</a>.
 * Otherwise, because compilation is not intermodular and because runtime is flattened, modules consist
 * of a single "default" module.
 */
public interface IModule
{
  IManifoldHost getHost();

  String getName();

  /**
   * The path[s] having source files that should be exposed to this module.
   */
  List<IDirectory> getSourcePath();

  List<IDirectory> getJavaClassPath();

  List<IDirectory> getOutputPath();

  IDirectory[] getExcludedPath();

  List<IDirectory> getCollectiveSourcePath();

  List<IDirectory> getCollectiveJavaClassPath();

  default IFileSystem getFileSystem()
  {
    return getHost().getFileSystem();
  }

  /**
   * @return A list of dependency modules.
   * The dependency graph must not have cycles.
   */
  List<Dependency> getDependencies();

  PathCache getPathCache();

  Set<ITypeManifold> getTypeManifolds();

  JavaFileObject produceFile( String fqn, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler );

  default Set<ITypeManifold> findTypeManifoldsFor( String fqn )
  {
    return findTypeManifoldsFor( fqn, null );
  }
  /**
   * Finds the set of type manifolds that contribute toward the definition of a given type.
   *
   * @param fqn       A fully qualified type name
   * @param predicate A predicate to filter the set of type manifolds available
   *
   * @return The set of type manifolds that contribute toward the definition of {@code fqn}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( String fqn, Predicate<ITypeManifold> predicate )
  {
    Set<ITypeManifold> tms = null;
    Set<ITypeManifold> typeManifolds = getTypeManifolds();
    for( ITypeManifold tm : typeManifolds )
    {
      if( (predicate == null || predicate.test( tm )) &&
          tm.isType( fqn ) )
      {
        tms = tms == null ? new HashSet<>( 2 ) : tms;
        tms.add( tm );
      }
    }
    return tms == null ? Collections.emptySet() : tms;
  }

  default Set<ITypeManifold> findTypeManifoldsFor( IFile file )
  {
    return findTypeManifoldsFor( file, null );
  }
  /**
   * Finds the set of type manifolds that handle a given resource file.
   *
   * @param file A resource file
   * @param predicate A predicate to filter the set of type manifolds available
   *
   * @return The set of type manifolds that handle {@code file}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( IFile file, Predicate<ITypeManifold> predicate )
  {
    Set<ITypeManifold> tms = null;
    Set<ITypeManifold> typeManifolds = getTypeManifolds();
    for( ITypeManifold tm : typeManifolds )
    {
      if( (predicate == null || predicate.test( tm )) &&
          tm.handlesFile( file ) )
      {
        tms = tms == null ? new HashSet<>( 2 ) : tms;
        tms.add( tm );
      }
    }
    return tms == null ? Collections.emptySet() : tms;
  }

  /**
   * Loads, but does not initialize, all type manifolds managed by this module.
   *
   * @return The complete set of type manifolds this module manages.
   */
  default SortedSet<ITypeManifold> loadTypeManifolds()
  {
    // note type manifolds are sorted via getTypeManifoldSorter(), hence the use of TreeSet
    SortedSet<ITypeManifold> typeManifolds = new TreeSet<>( getTypeManifoldSorter() );
    loadRegistered( typeManifolds );
    return typeManifolds;
  }

  /**
   * Supplemental type manifolds must follow others, this is so that a Supplemental
   * manifold in response to changes can be sure that side effects stemming from
   * Primary or Partial manifolds are deterministic and complete beforehand.
   * <p/>
   * Implementors <b>must</b> maintain this as the primary sort.
   */
  default Comparator<ITypeManifold> getTypeManifoldSorter()
  {
    //noinspection ComparatorMethodParameterNotUsed
    return (tm1, tm2) -> tm1.getContributorKind() == ContributorKind.Supplemental ? 1 : -1;
  }

  default List<String> getExcludedTypeManifolds()
  {
    String exclude = System.getProperty( "manifold.exclude" );
    if( exclude != null && !exclude.isEmpty() )
    {
      List<String> excluded = new ArrayList<>();
      for( StringTokenizer tokenizer = new StringTokenizer( exclude, "," ); tokenizer.hasMoreTokens(); )
      {
        String excludedTypeManifold = tokenizer.nextToken().trim();
        excluded.add( excludedTypeManifold );
      }
      return excluded;
    }
    return Collections.emptyList();
  }

  default void loadRegistered( Set<ITypeManifold> tms )
  {
    Set<ITypeManifold> registeredTms = new HashSet<>();
    ServiceUtil.loadRegisteredServices( registeredTms, ITypeManifold.class, getClass().getClassLoader() );

    // Exclude type manifolds listed in the "manifold.exclude" sys property
    List<String> excludedTypeManifolds = getExcludedTypeManifolds();
    tms.addAll( registeredTms.stream()
      .filter( tm -> tm.accept( this ) && !excludedTypeManifolds.contains( tm.getClass().getTypeName() ) )
      .collect( Collectors.toSet() ) );
  }
}
