package manifold.internal.host;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.def.FileSystemImpl;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.service.BaseService;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.runtime.UrlClassLoaderWrapper;
import manifold.util.BytecodeOptions;
import manifold.util.SourcePathUtil;
import manifold.util.concurrent.LocklessLazyVar;

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

  private DefaultSingleModule _module;
  private List<File> _classpath;
  private LocklessLazyVar<IFileSystem> _fileSystem = LocklessLazyVar.make(
    () ->
    {
      //noinspection ConstantConditions
      if( BytecodeOptions.JDWP_ENABLED.get() )
      {
        return new FileSystemImpl( IFileSystem.CachingMode.NO_CACHING );
      }
      return new FileSystemImpl( IFileSystem.CachingMode.FULL_CACHING );
    }
  );

  @Override
  public boolean isBootstrapped()
  {
    return _module != null;
  }

  private void initDirPaths( List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    // Must assign _module BEFORE initializeTypeManifolds() to prevent double bootstrapping
    _module = new DefaultSingleModule( classpath, sourcePath, outputPath );

    _module.initializeTypeManifolds();
  }

  public IFileSystem getFileSystem()
  {
    return _fileSystem.get();
  }

  public ClassLoader getActualClassLoader()
  {
    if( JavacPlugin.instance() == null )
    {
      // runtime
      return Thread.currentThread().getContextClassLoader();
    }
    // compile-time
    return ManifoldHost.class.getClassLoader();
  }

  public void bootstrap( List<File> sourcepath, List<File> classpath )
  {
    if( isBootstrapped() )
    {
      return;
    }

    preBootstrap();
    init( sourcepath, classpath );
  }

  public IModule getGlobalModule()
  {
    return _module;
  }

  public IModule getCurrentModule()
  {
    return _module;
  }

  public void resetLanguageLevel()
  {
  }

  public boolean isPathIgnored( String path )
  {
    return false;
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

  public JavaFileObject produceFile( String fqn, JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return module.produceFile( fqn, location, errorHandler );
  }

  @Override
  public void maybeAssignManifoldType( ClassLoader loader, String fqn, URL url, BiConsumer<String, Supplier<byte[]>> assigner )
  {
    Set<ITypeManifold> sps = getCurrentModule().findTypeManifoldsFor( fqn );
    if( !sps.isEmpty() )
    {
      assigner.accept( fqn, null );
    }
  }

  public void performLockedOperation( ClassLoader loader, Runnable operation )
  {
    operation.run();
  }

  public void initializeAndCompileNonJavaFiles( ProcessingEnvironment procEnv, JavaFileManager fileManager, List<String> files, Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<List<String>> outputPath )
  {
    List<String> cp = classpath.get().stream().filter( e -> !SourcePathUtil.excludeFromSourcePath( e ) ).collect( Collectors.toList() );
    Set<String> sp = sourcePath.get().stream().filter( e -> !SourcePathUtil.excludeFromSourcePath( e ) ).collect( Collectors.toSet() );
    List<String> op = outputPath.get();

    int i = 0;
    for( String p : op )
    {
      if( !cp.contains( p ) )
      {
        // ensure output path is in the classpath
        cp.add( i++, p );
      }
    }

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
    initPaths( cp, all, op );
  }

  private void initPaths( List<String> classpath, List<String> sourcePath, List<String> outputPath )
  {
    IFileSystem fs = ManifoldHost.getFileSystem();
    List<IDirectory> cp = classpath.stream().map( path -> fs.getIDirectory( new File( path ) ) ).collect( Collectors.toList() );
    List<IDirectory> sp = sourcePath.stream().map( path -> fs.getIDirectory( new File( path ) ) ).collect( Collectors.toList() );
    List<IDirectory> op = outputPath.stream().map( path -> fs.getIDirectory( new File( path ) ) ).collect( Collectors.toList() );
    initDirPaths( cp, sp, op );
  }

  public Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    return ((SimpleModule)getCurrentModule()).getChildrenOfNamespace( packageName );
  }

  public void init( List<File> sourcepath, List<File> classpath )
  {
    List<File> combined = new ArrayList<>();
    if( classpath != null )
    {
      combined.addAll( classpath );
    }
    combined.addAll( deriveClasspathFrom( SourcePathUtil.class ) );
    setPaths( sourcepath, combined );
  }

  private void setPaths( List<File> sourcepath, List<File> classpath )
  {
    classpath = new ArrayList<>( classpath );
    removeDups( classpath );

    if( classpath.equals( _classpath ) )
    {
      return;
    }

    _classpath = classpath;

    List<IDirectory> cp = createDefaultClassPath();
    cp.addAll( classpath.stream().map( file -> ManifoldHost.getFileSystem().getIDirectory( file ) ).collect( Collectors.toList() ) );
    removeDups( cp );
    List<IDirectory> sp = sourcepath.stream().map( file -> ManifoldHost.getFileSystem().getIDirectory( file ) ).filter( e -> !SourcePathUtil.excludeFromSourcePath( e.toJavaFile().getAbsolutePath() ) ).collect( Collectors.toList() );

    List<IDirectory> all = new ArrayList<>();
    for( IDirectory p : sp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    for( IDirectory p : cp )
    {
      if( !all.contains( p ) )
      {
        if( !SourcePathUtil.excludeFromSourcePath( p.toJavaFile().getAbsolutePath() ) )
        {
          all.add( p );
        }
      }
    }

    initDirPaths( cp, all, Collections.emptyList() );
  }

  private List<IDirectory> createDefaultClassPath()
  {
    List<String> vals = new ArrayList<>();
    vals.add( removeQuotes( System.getProperty( "java.class.path", "" ) ) );
    vals.add( System.getProperty( "sun.boot.class.path", "" ) );
    vals.add( System.getProperty( "java.ext.dirs", "" ) );

    return expand( vals );
  }

  private static List<IDirectory> expand( List<String> paths )
  {
    LinkedHashSet<IDirectory> expanded = new LinkedHashSet<>();
    for( String path : paths )
    {
      for( String pathElement : path.split( File.pathSeparator ) )
      {
        if( pathElement.length() > 0 )
        {
          File filePath = new File( pathElement );
          IDirectory resource = ManifoldHost.getFileSystem().getIDirectory( filePath );
          expanded.add( resource );
        }
      }
    }
    return new ArrayList<>( expanded );
  }

  /**
   * trims leading and/or trailing double quotes
   * we've only seen this behavior on linux/macOS
   */
  private static String removeQuotes( String classpath )
  {
    if( classpath.startsWith( "\"" ) )
    {
      classpath = classpath.substring( 1 );
    }
    if( classpath.endsWith( "\"" ) )
    {
      classpath = classpath.substring( 0, classpath.length() - 1 );
    }
    return classpath;
  }

  private static void removeDups( List classpath )
  {
    for( int i = classpath.size() - 1; i >= 0; i-- )
    {
      Object f = classpath.get( i );
      classpath.remove( i );
      if( !classpath.contains( f ) )
      {
        //noinspection unchecked
        classpath.add( i, f );
      }
    }
  }

  private List<File> deriveClasspathFrom( Class clazz )
  {
    List<File> ll = new LinkedList<>();
    ClassLoader loader = clazz.getClassLoader();
    while( loader != null )
    {
      UrlClassLoaderWrapper wrap = UrlClassLoaderWrapper.wrap( loader );
      if( wrap != null )
      {
        for( URL url : wrap.getURLs() )
        {
          try
          {
            File file = new File( url.toURI() );
            if( file.exists() && !ll.contains( file ) )
            {
              ll.add( file );
            }
          }
          catch( Exception e )
          {
            //ignore
          }
        }
      }
      loader = loader.getParent();
    }
//    addBootstrapClasses( ll );
    return ll;
  }
}
