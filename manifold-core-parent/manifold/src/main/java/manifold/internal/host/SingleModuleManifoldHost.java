package manifold.internal.host;

import java.util.List;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.def.FileSystemImpl;
import manifold.api.host.IModule;
import manifold.api.host.ITypeSystemListener;
import manifold.internal.javac.JavaParser;
import manifold.util.BytecodeOptions;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public abstract class SingleModuleManifoldHost extends AbstractManifoldHost
{
  private DefaultSingleModule _module;
  private ThreadLocal<JavaParser> _javaParser = new ThreadLocal<>();
  private LocklessLazyVar<IFileSystem> _fileSystem = LocklessLazyVar.make(
    () ->
    {
      //noinspection ConstantConditions
      if( BytecodeOptions.JDWP_ENABLED.get() )
      {
        return new FileSystemImpl( this, IFileSystem.CachingMode.NO_CACHING );
      }
      return new FileSystemImpl( this, IFileSystem.CachingMode.FULL_CACHING );
    }
  );

  public IFileSystem getFileSystem()
  {
    return _fileSystem.get();
  }

  @Override
  public JavaParser getJavaParser()
  {
    if( _javaParser.get() == null )
    {
      _javaParser.set( new JavaParser( this ) );
    }
    return _javaParser.get();
  }

  public IModule getSingleModule()
  {
    return _module;
  }

  void createSingleModule( List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    if( _module != null )
    {
      throw new IllegalStateException();
    }

    // Must assign _module BEFORE initializeTypeManifolds() to prevent double bootstrapping
    _module = new DefaultSingleModule( this, classpath, sourcePath, outputPath );
    _module.initializeTypeManifolds();
  }

  public void addTypeSystemListenerAsWeakRef( Object ctx, ITypeSystemListener listener )
  {
    // only relevant for environments where types change e.g., Manifold IJ plugin
  }
}
