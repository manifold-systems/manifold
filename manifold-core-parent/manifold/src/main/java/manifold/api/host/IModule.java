package manifold.api.host;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
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
import manifold.api.darkj.DarkJavaTypeManifold;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.cache.PathCache;
import manifold.api.image.ImageTypeManifold;
import manifold.api.properties.PropertiesTypeManifold;
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

  /**
   * Finds the set of type manifolds that contribute toward the definition of a given type.
   *
   * @param fqn        A fully qualified type name
   * @param predicates Zero or more predicates to filter the set of type manifolds available
   *
   * @return The set of type manifolds that contribute toward the definition of {@code fqn}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( String fqn, Predicate<ITypeManifold>... predicates )
  {
    Set<ITypeManifold> tms = new HashSet<>( 2 );
    Set<ITypeManifold> typeManifolds = getTypeManifolds();
    if( predicates != null && predicates.length > 0 )
    {
      typeManifolds = typeManifolds.stream()
        .filter( e -> Arrays.stream( predicates )
          .anyMatch( p -> p.test( e ) ) )
        .collect( Collectors.toSet() );
    }
    for( ITypeManifold tm : typeManifolds )
    {
      if( tm.isType( fqn ) )
      {
        tms.add( tm );
      }
    }
    return tms;
  }

  /**
   * Finds the set of type manifolds that handle a given resource file.
   *
   * @param file A resource file
   *
   * @return The set of type manifolds that handle {@code file}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( IFile file )
  {
    Set<ITypeManifold> tms = new HashSet<>( 2 );
    for( ITypeManifold tm : getTypeManifolds() )
    {
      if( tm.handlesFile( file ) )
      {
        tms.add( tm );
      }
    }
    return tms;
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
    loadBuiltIn( typeManifolds );
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

  /**
   * Loads, but does not initialize, all <i>built-in</i> type manifolds managed by this module.
   * A built-in type manifold is not registered as a Java service, instead it is constructed directly.
   */
  default void loadBuiltIn( Set<ITypeManifold> tms )
  {
    List<String> excludedTypeManifolds = getExcludedTypeManifolds();
    addBuiltIn( PropertiesTypeManifold.class, tms, excludedTypeManifolds );
    addBuiltIn( ImageTypeManifold.class, tms, excludedTypeManifolds );
    addBuiltIn( DarkJavaTypeManifold.class, tms, excludedTypeManifolds );
  }

  default void addBuiltIn( Class<? extends ITypeManifold> tmClass, Set<ITypeManifold> tms, List<String> excludedTypeManifolds )
  {
    if( !excludedTypeManifolds.contains( tmClass.getTypeName() ) )
    {
      try
      {
        Constructor<? extends ITypeManifold> declaredConstructor = tmClass.getDeclaredConstructor();
        if( declaredConstructor == null )
        {
          throw new IllegalStateException( "Type manifold class '" + tmClass.getTypeName() + "' does not define an accessible default constructor" );
        }
        ITypeManifold tm = declaredConstructor.newInstance();
        if( tm.accept( this ) )
        {
          tms.add( tm );
        }
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }
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
