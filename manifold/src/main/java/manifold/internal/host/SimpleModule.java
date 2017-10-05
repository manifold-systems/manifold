package manifold.internal.host;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.host.Dependency;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;
import manifold.api.image.ImageTypeManifold;
import manifold.api.properties.PropertiesTypeManifold;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.javac.GeneratedJavaStubFileObject;


import static manifold.api.type.ITypeManifold.ProducerKind.Partial;
import static manifold.api.type.ITypeManifold.ProducerKind.Primary;

/**
 */
@SuppressWarnings("WeakerAccess")
public abstract class SimpleModule implements ITypeLoader, IModule
{
  private List<IDirectory> _classpath;
  private List<IDirectory> _sourcePath;
  private List<IDirectory> _outputPath;
  private Set<ITypeManifold> _typeManifolds;

  public SimpleModule( List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    _classpath = classpath;
    _sourcePath = sourcePath;
    _outputPath = outputPath;
  }

  @Override
  public IModule getModule()
  {
    return this;
  }

  @Override
  public List<IDirectory> getSourcePath()
  {
    return _sourcePath;
  }

  @Override
  public List<IDirectory> getJavaClassPath()
  {
    return _classpath;
  }

  @Override
  public List<IDirectory> getOutputPath()
  {
    return _outputPath;
  }

  @Override
  public IDirectory[] getExcludedPath()
  {
    return new IDirectory[0];
  }

  @Override
  public List<IDirectory> getCollectiveSourcePath()
  {
    return getSourcePath();
  }

  @Override
  public List<IDirectory> getCollectiveJavaClassPath()
  {
    return getJavaClassPath();
  }

  @Override
  public List<Dependency> getDependencies()
  {
    return Collections.emptyList();
  }

  public Set<ITypeManifold> getTypeManifolds()
  {
    return _typeManifolds;
  }

  public JavaFileObject produceFile( String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    Set<ITypeManifold> sps = findTypeManifoldsFor( fqn );
    return sps.isEmpty() ? null : new GeneratedJavaStubFileObject( fqn, () -> compoundProduce( sps, fqn, errorHandler ) );
  }

  private String compoundProduce( Set<ITypeManifold> sps, String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    ITypeManifold found = null;
    String result = "";
    for( ITypeManifold sp : sps )
    {
      if( sp.getProducerKind() == Primary ||
          sp.getProducerKind() == Partial )
      {
        if( found != null && (found.getProducerKind() == Primary || sp.getProducerKind() == Primary) )
        {
          //## todo: how better to handle this?
          throw new UnsupportedOperationException( "The type, " + fqn + ", has conflicting source producers: '" +
                                                   found.getClass().getName() + "' and '" + sp.getClass().getName() + "'" );
        }
        found = sp;
        result = sp.produce( fqn, result, errorHandler );
      }
    }
    for( ITypeManifold sp : sps )
    {
      if( sp.getProducerKind() == ITypeManifold.ProducerKind.Supplemental )
      {
        result = sp.produce( fqn, result, errorHandler );
      }
    }
    return result;
  }

  public Set<ITypeManifold> findTypeManifoldsFor( String fqn )
  {
    Set<ITypeManifold> sps = new HashSet<>( 2 );
    for( ITypeManifold sp : getTypeManifolds() )
    {
      if( sp.isType( fqn ) )
      {
        sps.add( sp );
      }
    }
    return sps;
  }

  public Set<ITypeManifold> findTypeManifoldsFor( IFile file )
  {
    Set<ITypeManifold> sps = new HashSet<>( 2 );
    for( ITypeManifold sp : getTypeManifolds() )
    {
      if( sp.handlesFile( file ) )
      {
        sps.add( sp );
      }
    }
    return sps;
  }

  protected void initializeTypeManifolds()
  {
    if( _typeManifolds != null )
    {
      return;
    }

    synchronized( this )
    {
      if( _typeManifolds != null )
      {
        return;
      }

      Set<ITypeManifold> typeManifolds = new HashSet<>();
      addBuiltIn( typeManifolds );
      addRegistered( typeManifolds );
      _typeManifolds = typeManifolds;
    }
  }

  private void addBuiltIn( Set<ITypeManifold> sps )
  {
    ITypeManifold sp = new PropertiesTypeManifold();
    sp.init( this );
    sps.add( sp );

    sp = new ImageTypeManifold();
    sp.init( this );
    sps.add( sp );
  }

  protected void addRegistered( Set<ITypeManifold> sps )
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
          sp.init( this );
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

  private boolean isAbsent( Set<ITypeManifold> sps, ITypeManifold sp )
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

  public Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    Set<TypeName> children = new HashSet<>();
    for( ITypeManifold sp : getTypeManifolds() )
    {
      Collection<TypeName> typeNames = sp.getTypeNames( packageName );
      children.addAll( typeNames );
    }
    return children;
  }
}
