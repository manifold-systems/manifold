package manifold.internal.host;

import java.util.*;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.darkj.DarkJavaTypeManifold;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.PathCache;
import manifold.api.host.Dependency;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;
import manifold.api.image.ImageTypeManifold;
import manifold.api.properties.PropertiesTypeManifold;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.javac.GeneratedJavaStubFileObject;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.internal.javac.SourceSupplier;
import manifold.util.JavacDiagnostic;
import manifold.util.concurrent.LocklessLazyVar;


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
  private LocklessLazyVar<PathCache> _pathCache;

  public SimpleModule( List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    _classpath = classpath;
    _sourcePath = sourcePath;
    _outputPath = outputPath;
    _pathCache = LocklessLazyVar.make( this::makePathCache );
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
  protected void setJavaClassPath( List<IDirectory> cp )
  {
    _classpath = cp;
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

  public PathCache getPathCache()
  {
    return _pathCache.get();
  }

  public Set<ITypeManifold> getTypeManifolds()
  {
    return _typeManifolds;
  }

  public JavaFileObject produceFile( String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    Set<ITypeManifold> sps = findTypeManifoldsFor( fqn );
    return sps.isEmpty() ? null : new GeneratedJavaStubFileObject( fqn, new SourceSupplier( sps, () -> compoundProduce( sps, fqn, errorHandler ) ) );
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
          List<IFile> files = sp.findFilesForType( fqn );
          JavaFileObject file = new SourceJavaFileObject( files.get( 0 ).toURI() );
          errorHandler.report( new JavacDiagnostic( file, Diagnostic.Kind.ERROR, 0, 1, 1,
                                                    "The type, " + fqn + ", has conflicting type manifolds:\n" +
                                                    "'" + found.getClass().getName() + "' and '" + sp.getClass().getName() + "'.\n" +
                                                    "Either two or more resource files have the same base name or the project depends on two or more type manifolds that target the same resource type.\n" +
                                                    "If the former, consider renaming one or more of the resource files.\n" +
                                                    "If the latter, you must remove one or more of the type manifold libraries." ) );
        }
        else
        {
          found = sp;
          result = sp.produce( fqn, result, errorHandler );
        }
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

  public void initializeTypeManifolds()
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

    sp = new DarkJavaTypeManifold();
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

  private PathCache makePathCache()
  {
    return new PathCache( this, this::makeModuleSourcePath, () -> {} );
  }

  private List<IDirectory> makeModuleSourcePath()
  {
    return getSourcePath().stream()
      .filter( dir -> Arrays.stream( getExcludedPath() )
        .noneMatch( excludeDir -> excludeDir.equals( dir ) ) )
      .collect( Collectors.toList() );
  }
}
