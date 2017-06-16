package manifold.internal.host;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.host.Dependency;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;
import manifold.api.image.ImageSourceProducer;
import manifold.api.json.JsonImplSourceProducer;
import manifold.api.properties.PropertiesSourceProducer;
import manifold.api.sourceprod.ISourceProducer;
import manifold.api.sourceprod.TypeName;
import manifold.internal.javac.GeneratedJavaStubFileObject;


import static manifold.api.sourceprod.ISourceProducer.ProducerKind.*;

/**
 */
@SuppressWarnings("WeakerAccess")
public abstract class SimpleModule implements ITypeLoader, IModule
{
  private List<IDirectory> _classpath;
  private List<IDirectory> _sourcePath;
  private IDirectory _outputPath;
  private Set<ISourceProducer> _sourceProducers;

  public SimpleModule( List<IDirectory> classpath, List<IDirectory> sourcePath, IDirectory outputPath )
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
  public IDirectory getOutputPath()
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

  public Set<ISourceProducer> getSourceProducers()
  {
    return _sourceProducers;
  }

  public JavaFileObject produceFile( String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    Set<ISourceProducer> sps = findSourceProducersFor( fqn );
    return sps.isEmpty() ? null : new GeneratedJavaStubFileObject( fqn, () -> compoundProduce( sps, fqn, errorHandler ) );
  }

  private String compoundProduce( Set<ISourceProducer> sps, String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    ISourceProducer found = null;
    String result = "";
    for( ISourceProducer sp: sps )
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
    for( ISourceProducer sp: sps )
    {
      if( sp.getProducerKind() == ISourceProducer.ProducerKind.Supplemental )
      {
        result = sp.produce( fqn, result, errorHandler );
      }
    }
    return result;
  }

  public Set<ISourceProducer> findSourceProducersFor( String fqn )
  {
    Set<ISourceProducer> sps = new HashSet<>( 2 );
    for( ISourceProducer sp: getSourceProducers() )
    {
      if( sp.isType( fqn ) )
      {
        sps.add( sp );
      }
    }
    return sps;
  }

  public Set<ISourceProducer> findSourceProducersFor( IFile file )
  {
    Set<ISourceProducer> sps = new HashSet<>( 2 );
    for( ISourceProducer sp: getSourceProducers() )
    {
      if( sp.handlesFile( file ) )
      {
        sps.add( sp );
      }
    }
    return sps;
  }

  protected void initializeSourceProducers()
  {
    if( _sourceProducers != null )
    {
      return;
    }

    synchronized( this )
    {
      if( _sourceProducers != null )
      {
        return;
      }

      Set<ISourceProducer> sourceProducers = new HashSet<>();
      addBuiltIn( sourceProducers );
      addRegistered( sourceProducers );
      _sourceProducers = sourceProducers;
    }
  }

  private void addBuiltIn( Set<ISourceProducer> sps )
  {
    ISourceProducer sp = new JsonImplSourceProducer();
    sp.init( this );
    sps.add( sp );

    sp = new PropertiesSourceProducer();
    sp.init( this );
    sps.add( sp );

    sp = new ImageSourceProducer();
    sp.init( this );
    sps.add( sp );
  }

  protected void addRegistered( Set<ISourceProducer> sps )
  {
    // Load from Thread Context Loader
    // (currently the IJ plugin creates a loaders for accessing source producers from project classpath)
    ServiceLoader<ISourceProducer> loader = ServiceLoader.load( ISourceProducer.class );
    Iterator<ISourceProducer> iterator = loader.iterator();
    if( iterator.hasNext() )
    {
      while( iterator.hasNext() )
      {
        ISourceProducer sp = iterator.next();
        sp.init( this );
        sps.add( sp );
      }
    }

    if( Thread.currentThread().getContextClassLoader() != getClass().getClassLoader() )
    {
      // Also load from this loader
      loader = ServiceLoader.load( ISourceProducer.class, getClass().getClassLoader() );
      for( ISourceProducer sp : loader )
      {
        sp.init( this );
        sps.add( sp );
      }
    }
  }

  public Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    Set<TypeName> children = new HashSet<>();
    for( ISourceProducer sp: getSourceProducers() )
    {
      Collection<TypeName> typeNames = sp.getTypeNames( packageName );
      children.addAll( typeNames );
    }
    return children;
  }
}
