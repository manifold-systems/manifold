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
 */
public interface ITypeLoader
{
  IModule getModule();

  default Set<ITypeManifold> findTypeManifoldsFor( String fqn )
  {
    Set<ITypeManifold> sps = new HashSet<>( 2 );
    for( ITypeManifold sp : getModule().getTypeManifolds() )
    {
      if( sp.isType( fqn ) )
      {
        sps.add( sp );
      }
    }
    return sps;
  }

  default Set<ITypeManifold> findTypeManifoldsFor( IFile file )
  {
    Set<ITypeManifold> sps = new HashSet<>( 2 );
    for( ITypeManifold sp : getModule().getTypeManifolds() )
    {
      if( sp.handlesFile( file ) )
      {
        sps.add( sp );
      }
    }
    return sps;
  }

  default Set<ITypeManifold> loadTypeManifolds()
  {
    Set<ITypeManifold> typeManifolds = new HashSet<>();
    loadBuiltIn( typeManifolds );
    loadRegistered( typeManifolds );
    return typeManifolds;
  }

  default void loadBuiltIn( Set<ITypeManifold> sps )
  {
    ITypeManifold sp = new PropertiesTypeManifold();
    sps.add( sp );

    sp = new ImageTypeManifold();
    sps.add( sp );

    sp = new DarkJavaTypeManifold();
    sps.add( sp );
  }

  default void loadRegistered( Set<ITypeManifold> sps )
  {
    // Load from Thread Context Loader
    // (currently the IJ plugin creates loaders for accessing source producers from project classpath)
    ServiceLoader<ITypeManifold> loader = ServiceLoader.load( ITypeManifold.class );
    Iterator<ITypeManifold> iterator = loader.iterator();
    if( iterator.hasNext() )
    {
      while( iterator.hasNext() )
      {
        ITypeManifold sp = iterator.next();
        sp.init( this );
        sps.add( sp );
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
          ITypeManifold sp = iterator.next();
          if( isAbsent( sps, sp ) )
          {
            sps.add( sp );
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

  default boolean isAbsent( Set<ITypeManifold> sps, ITypeManifold sp )
  {
    for( ITypeManifold existingSp: sps )
    {
      if( existingSp.getClass().equals( sp.getClass() ) )
      {
        return false;
      }
    }
    return true;
  }
}
