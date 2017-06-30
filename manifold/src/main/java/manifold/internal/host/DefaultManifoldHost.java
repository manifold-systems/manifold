package manifold.internal.host;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.def.FileSystemImpl;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.service.BaseService;
import manifold.api.sourceprod.ISourceProducer;
import manifold.api.sourceprod.TypeName;
import manifold.util.BytecodeOptions;

/**
 */
public class DefaultManifoldHost extends BaseService implements IManifoldHost
{
  private static final String JAVA_KEYWORDS[] = {
    "abstract", "assert", "boolean",
    "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "extends", "false",
    "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native",
    "new", "null", "package", "private", "protected", "public",
    "return", "short", "static", "strictfp", "super", "switch",
    "synchronized", "this", "throw", "throws", "transient", "true",
    "try", "void", "volatile", "while"};

  public IFileSystem getFileSystem()
  {
    if( BytecodeOptions.JDWP_ENABLED.get() )
    {
      return new FileSystemImpl( IFileSystem.CachingMode.NO_CACHING );
    }
    return new FileSystemImpl( IFileSystem.CachingMode.FULL_CACHING );
  }

  public ClassLoader getActualClassLoader()
  {
    return Thread.currentThread().getContextClassLoader();
  }

  public void bootstrap()
  {
    if( Manifold.isBootstrapped() )
    {
      return;
    }

    Manifold.instance().init();
  }

  public IModule getGlobalModule()
  {
    return Manifold.instance().getModule();
  }

  public IModule getCurrentModule()
  {
    return Manifold.instance().getModule();
  }

  public void resetLanguageLevel()
  {
  }

  public boolean isPathIgnored( String path )
  {
    return false;
  }

  public ITypeLoader getLoader( IFile file, IModule module )
  {
    return Manifold.instance().getModule();
  }

  public String[] getAllReservedWords()
  {
    return JAVA_KEYWORDS;
  }

  public Bindings createBindings()
  {
    return new SimpleBindings();
  }

  public void addTypeLoaderListenerAsWeakRef( Object ctx, ITypeLoaderListener listener )
  {
    // assumed runtime (as opposed to compile-time), thus no changes to listen to
  }

  public JavaFileObject produceFile( String fqn, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return module.produceFile( fqn, errorHandler );
  }

  @Override
  public void maybeAssignManifoldType( ClassLoader loader, String fqn, URL url, BiConsumer<String, Supplier<byte[]>> assigner )
  {
    Set<ISourceProducer> sps = getCurrentModule().findSourceProducersFor( fqn );
    if( !sps.isEmpty() )
    {
      assigner.accept( fqn, null );
    }
  }

  public void performLockedOperation( ClassLoader loader, Runnable operation )
  {
    operation.run();
  }

  public void initializeAndCompileNonJavaFiles( JavacProcessingEnvironment jpe, JavaFileManager fileManager, List<String> files, Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<String> outputPath )
  {
    List<String> cp = classpath.get();
    Set<String> sp = sourcePath.get();

    List<String> all = new ArrayList<>();
    for( String p : sp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    for( String p : cp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    Manifold.instance().initPaths( cp, all, outputPath.get() );
  }

  public Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    return Manifold.instance().getModule().getChildrenOfNamespace( packageName );
  }

}
