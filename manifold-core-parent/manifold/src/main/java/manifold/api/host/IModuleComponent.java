package manifold.api.host;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import manifold.api.darkj.DarkJavaTypeManifold;
import manifold.api.fs.IFile;
import manifold.api.image.ImageTypeManifold;
import manifold.api.properties.PropertiesTypeManifold;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.util.ServiceUtil;

/**
 * A component of a {@link IModule}.  Normally a module is itself a module component, however some
 * {@link IManifoldHost} implementors may need to subdivide a module in terms of type manifold
 * management. For instance, the Gosu host handles both Java and Gosu based type manifolds per
 * module, thus it separates handling of type manifolds by {@link manifold.api.type.ISourceKind}
 * where it creates two instances of {@link IModuleComponent} per module, one for managing Java type manifolds
 * and one for Gosu.
 */
public interface IModuleComponent
{
  /**
   * @return The module to which this component belongs
   */
  IModule getModule();

  /**
   * Finds the set of type manifolds that contribute toward the definition of a given type.
   *
   * @param fqn A fully qualified type name
   * @param predicates Zero or more predicates to filter the set of type manifolds available
   * @return The set of type manifolds that contribute toward the definition of {@code fqn}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( String fqn, Predicate<ITypeManifold>... predicates )
  {
    Set<ITypeManifold> tms = new HashSet<>( 2 );
    Set<ITypeManifold> typeManifolds = getModule().getTypeManifolds();
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
   * @return The set of type manifolds that handle {@code file}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( IFile file )
  {
    Set<ITypeManifold> tms = new HashSet<>( 2 );
    for( ITypeManifold tm : getModule().getTypeManifolds() )
    {
      if( tm.handlesFile( file ) )
      {
        tms.add( tm );
      }
    }
    return tms;
  }

  /**
   * Loads, but does not initialize, all type manifolds managed by this module container.
   *
   * @return The complete set of type manifolds this module container manages.
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
   * Loads, but does not initialize, all <i>built-in</i> type manifolds managed by this module container.
   * A built-in type manifold is not registered as a Java service, instead it is constructed directly.
   */
  default void loadBuiltIn( Set<ITypeManifold> tms )
  {
    ITypeManifold tm = new PropertiesTypeManifold();
    tms.add( tm );

    tm = new ImageTypeManifold();
    tms.add( tm );

    tm = new DarkJavaTypeManifold();
    tms.add( tm );
  }

  default void loadRegistered( Set<ITypeManifold> tms )
  {
    ServiceUtil.loadRegisteredServices( tms, ITypeManifold.class, getClass().getClassLoader() );
  }
}
