package manifold.internal.host;

import java.util.*;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.PathCache;
import manifold.api.host.Dependency;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;
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

      _typeManifolds = ITypeLoader.super.loadTypeManifolds();
      _typeManifolds.forEach( tm -> tm.init( this ) );
    }
  }

  public Set<ITypeManifold> findTypeManifoldsFor( String fqn )
  {
    return ITypeLoader.super.findTypeManifoldsFor( fqn );
  }

  @Override
  public Set<ITypeManifold> findTypeManifoldsFor( IFile file )
  {
    return ITypeLoader.super.findTypeManifoldsFor( file );
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
