package manifold.api.host;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import manifold.api.darkj.DarkJavaTypeManifold;
import manifold.api.fs.IFile;
import manifold.api.image.ImageTypeManifold;
import manifold.api.properties.PropertiesTypeManifold;
import manifold.api.type.ITypeManifold;

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
   * @return The set of type manifolds that contribute toward the definition of {@code fqn}
   */
  default Set<ITypeManifold> findTypeManifoldsFor( String fqn )
  {
    Set<ITypeManifold> tms = new HashSet<>( 2 );
    for( ITypeManifold tm : getModule().getTypeManifolds() )
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
  default Set<ITypeManifold> loadTypeManifolds()
  {
    Set<ITypeManifold> typeManifolds = new HashSet<>();
    loadBuiltIn( typeManifolds );
    loadRegistered( typeManifolds );
    return typeManifolds;
  }

  /**
   * Loads, but does not initialize, all <i>built-in</i> type manifolds managed by this module container.
   * A built-in type manifold is not registered as a Java service, instead it is constructed directly.
   *
   * @return The built-in type manifolds this module container manages.
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

  /**
   * Loads, but does not initialize, all <i>registered</i>type manifolds managed by this module container.
   * A registered type manifold is discoverable in the META-INF/ directory as specified by {@link ServiceLoader}.
   *
   * @return The registered type manifolds this module container manages.
   */
  default void loadRegistered( Set<ITypeManifold> tms )
  {
    // Load from Thread Context Loader
    // (currently the IJ plugin creates loaders for accessing source producers from project classpath)

    ServiceLoader<ITypeManifold> loader = ServiceLoader.load( ITypeManifold.class );
    Iterator<ITypeManifold> iterator = loader.iterator();
    if( iterator.hasNext() )
    {
      while( iterator.hasNext() )
      {
        try
        {
          ITypeManifold sp = iterator.next();
          tms.add( sp );
        }
        catch( ServiceConfigurationError e )
        {
          // not in the loader, check thread ctx loader next
        }
      }
    }

    if( Thread.currentThread().getContextClassLoader() != getClass().getClassLoader() )
    {
      // Also load from this loader
      loader = ServiceLoader.load( ITypeManifold.class, getClass().getClassLoader() );
      for( iterator = loader.iterator(); iterator.hasNext(); )
      {
        try
        {
          ITypeManifold tm = iterator.next();
          if( isAbsent( tms, tm ) )
          {
            tms.add( tm );
          }
        }
        catch( ServiceConfigurationError e )
        {
          // avoid chicken/egg errors from attempting to build a module that self-registers a source producer
          // it's important to allow a source producer module to specify its xxx.ITypeManifold file in its META-INF
          // directory so that users of the source producer don't have to
        }
      }
    }
  }

  /**
   * @return True if {@code sp} is not contained within {@code sps}
   */
  default boolean isAbsent( Set<ITypeManifold> typeManifolds, ITypeManifold tm )
  {
    for( ITypeManifold existingSp: typeManifolds )
    {
      if( existingSp.getClass().equals( tm.getClass() ) )
      {
        return false;
      }
    }
    return true;
  }
}
