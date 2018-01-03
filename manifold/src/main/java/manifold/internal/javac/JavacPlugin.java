package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import manifold.internal.host.ManifoldHost;
import manifold.util.IssueMsg;
import manifold.util.JavacDiagnostic;
import manifold.util.Pair;
import sun.misc.Unsafe;

/**
 */
public class JavacPlugin implements Plugin, TaskListener
{
  private static final String GOSU_SOURCE_FILES = "gosu.source.files";
  private static Class<?> CLASSFINDER_CLASS = null;
  public static final boolean IS_JAVA_8;
  static {
    try
    {
      // ClassFinder is new in Java 9, its presence indicates Java 9 or later
      CLASSFINDER_CLASS = Class.forName( "com.sun.tools.javac.code.ClassFinder", false, ClassReader.class.getClassLoader() );
      disableJava9IllegalAccessWarning();
    }
    catch( Throwable ignore )
    {
    }
    IS_JAVA_8 = CLASSFINDER_CLASS == null;
  }
  private static JavacPlugin INSTANCE;

  private Context _ctx;
  private JavaFileManager _fileManager;
  private BasicJavacTask _javacTask;
  private Set<Pair<String, JavaFileObject>> _javaInputFiles;
  private List<String> _gosuInputFiles;
  private TreeMaker _treeMaker;
  private JavacElements _javacElements;
  private TypeProcessor _typeProcessor;
  private IssueReporter _issueReporter;
  private ManifoldJavaFileManager _manFileManager;
  private boolean _initialized;

  public static JavacPlugin instance()
  {
    return INSTANCE;
  }

  public JavacPlugin()
  {
    INSTANCE = this;
  }

  @Override
  public String getName()
  {
    return "Manifold";
  }

  @Override
  public void init( JavacTask task, String... args )
  {
    _javacTask = (BasicJavacTask)task;
    if( isCompilingCore() )
    {
      // Do not apply the plugin on Manifold core itself
      // Note this can only happen during incremental compile when JavacPlugin.class was previously compiled and in
      // the classpath but other parts of core manifold were changed and need compilation (the placeholder plugin is'nt
      // used in this case).
      JavacProcessingEnvironment.instance( getContext() ).getMessager().printMessage( Diagnostic.Kind.NOTE, "Bypassing JavacPlugin during incremental compilation of Manifold core" );
      return;
    }
    hijackJavacFileManager();
    task.addTaskListener( this );
  }

  private boolean isCompilingCore()
  {
    String outputPath = deriveClassOutputPath();
    return outputPath.contains( File.separatorChar + "manifold" + File.separatorChar + "target" + File.separatorChar );
  }

  @SuppressWarnings("WeakerAccess")
  public Context getContext()
  {
    return _ctx;
  }

  @SuppressWarnings("WeakerAccess")
  public JavaFileManager getJavaFileManager()
  {
    return _fileManager;
  }

  @SuppressWarnings("WeakerAccess")
  public ManifoldJavaFileManager getManifoldFileManager()
  {
    return _manFileManager;
  }

  @SuppressWarnings({"WeakerAccess", "unused"})
  public BasicJavacTask getJavacTask()
  {
    return _javacTask;
  }

  @SuppressWarnings("unused")
  public Set<Pair<String, JavaFileObject>> getJavaInputFiles()
  {
    return _javaInputFiles;
  }

  @SuppressWarnings("unused")
  public List<String> getGosuInputFiles()
  {
    return _gosuInputFiles;
  }

  @SuppressWarnings("WeakerAccess")
  public TreeMaker getTreeMaker()
  {
    return _treeMaker;
  }

  @SuppressWarnings("WeakerAccess")
  public JavacElements getJavacElements()
  {
    return _javacElements;
  }

  @SuppressWarnings("unused")
  public TypeProcessor getTypeProcessor()
  {
    return _typeProcessor;
  }

  @SuppressWarnings("WeakerAccess")
  public IssueReporter getIssueReporter()
  {
    return _issueReporter;
  }

  private void hijackJavacFileManager()
  {
    if( !(_fileManager instanceof ManifoldJavaFileManager) && _manFileManager == null )
    {
      _ctx = _javacTask.getContext();
      _fileManager = _ctx.get( JavaFileManager.class );
      _javaInputFiles = new HashSet<>();
      _gosuInputFiles = fetchGosuInputFiles();
      _treeMaker = TreeMaker.instance( _ctx );
      _javacElements = JavacElements.instance( _ctx );
      _typeProcessor = new TypeProcessor( _javacTask );
      _issueReporter = new IssueReporter( Log.instance( getContext() ) );

      injectManFileManager();
    }
  }

  private void injectManFileManager()
  {
    _manFileManager = new ManifoldJavaFileManager( _fileManager, _ctx, true );
    _ctx.put( JavaFileManager.class, (JavaFileManager)null );
    _ctx.put( JavaFileManager.class, _manFileManager );

    try
    {
      Field field;
      if( IS_JAVA_8 )
      {
        field = ClassReader.class.getDeclaredField( "fileManager" );
        field.setAccessible( true );
        field.set( ClassReader.instance( getContext() ), _manFileManager );
      }
      else
      {
        field = CLASSFINDER_CLASS.getDeclaredField( "fileManager" );
        field.setAccessible( true );
        field.set( CLASSFINDER_CLASS.getMethod( "instance", Context.class ).invoke( null, getContext() ), _manFileManager );

        field = CLASSFINDER_CLASS.getDeclaredField( "preferSource" );
        field.setAccessible( true );
        field.set( CLASSFINDER_CLASS.getMethod( "instance", Context.class ).invoke( null, getContext() ), true );
      }

      field = ClassWriter.class.getDeclaredField( "fileManager" );
      field.setAccessible( true );
      field.set( ClassWriter.instance( getContext() ), _manFileManager );

      field = Enter.class.getDeclaredField( "fileManager" );
      field.setAccessible( true );
      field.set( Enter.instance( getContext() ), _manFileManager );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private List<String> deriveOutputPath()
  {
    Set<String> paths = new HashSet<>();
    String outputPath = deriveClassOutputPath();

    String path = outputPath.replace( File.separatorChar, '/' );
    if( path.endsWith( "/" ) )
    {
      path = path.substring( 0, path.length() - 1 );
    }
    if( path.endsWith( "/java/main" ) )
    {
      String javaPath = path.substring( 0, path.lastIndexOf( "/main" ) );
      File javaDir = new File( javaPath );
      //noinspection ConstantConditions
      for( File file : javaDir.listFiles() )
      {
        if( file.isDirectory() )
        {
          paths.add( file.getAbsolutePath() );
        }
      }
      path = path.substring( 0, path.lastIndexOf( "/java/main" ) );
      if( path.endsWith( "/classes" ) )
      {
        String resources = path.substring( 0, path.lastIndexOf( "/classes" ) );
        resources += "/resources/main";
        File resourcesDir = new File( resources );
        if( resourcesDir.isDirectory() )
        {
          paths.add( resourcesDir.getAbsolutePath() );
        }
      }
    }

    if( paths.isEmpty() )
    {
      paths.add( outputPath );
    }

    return new ArrayList<>( paths );
  }

  private String deriveClassOutputPath()
  {
    try
    {
      String ping = "__dummy__";
      //JavaFileObject classFile = _jpe.getFiler().createClassFile( ping );
      JavaFileObject classFile = _javacTask.getContext().get( JavaFileManager.class ).getJavaFileForOutput( StandardLocation.CLASS_OUTPUT, ping, JavaFileObject.Kind.CLASS, null );
      if( !isPhysicalFile( classFile ) )
      {
        return "";
      }
      File dummyFile = new File( classFile.toUri() );
      String path = dummyFile.getAbsolutePath();
      path = path.substring( 0, path.length() - (File.separatorChar + ping + ".class").length() );
      return path;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private List<String> deriveClasspath()
  {
    URLClassLoader classLoader = (URLClassLoader)_javacTask.getContext().get( JavaFileManager.class ).getClassLoader( StandardLocation.CLASS_PATH );
    URL[] classpathUrls = classLoader.getURLs();
    List<String> paths = Arrays.stream( classpathUrls )
      .map( url ->
            {
              try
              {
                return new File( url.toURI() ).getAbsolutePath();
              }
              catch( URISyntaxException e )
              {
                throw new RuntimeException( e );
              }
            } ).collect( Collectors.toList() );
    return removeBadPaths( paths );
  }

  private List<String> removeBadPaths( List<String> paths )
  {
    // Remove a path that is a parent of another path.
    // For instance, "/foo/." is a parent of "/foo/classes", this must be unintentional

    List<String> actualPaths = new ArrayList<>();
    outer:
    for( String path : paths )
    {
      for( String p : paths )
      {
        //noinspection StringEquality
        String unmodifiedPath = path;
        if( path.endsWith( File.separator + '.' ) )
        {
          path = path.substring( 0, path.length() - 2 );
        }
        if( !p.equals( unmodifiedPath ) && p.startsWith( path ) )
        {
          continue outer;
        }
      }
      actualPaths.add( path );
    }
    return actualPaths;
  }

  private Set<String> deriveSourcePath()
  {
    Set<String> sourcePath = new HashSet<>();
    deriveSourcePath( _javaInputFiles, sourcePath );
    maybeAddResourcePath( sourcePath );
    return sourcePath;
  }

  /**
   * Add the Maven resource path from the conventional place.  This is not necessary because
   * resources are copied to the output directory, which is also part of the source path for
   * compilation.  However, having the resources directory in the path facilitates error
   * reporting in IDEs such as IntelliJ where there are hyper links directly to the errant
   * source file in the compilation results. Without the resources path, this link leads to
   * the output dir, which is confusing.
   */
  private void maybeAddResourcePath( Set<String> sourcePath )
  {
    String resourcePath = null;
    for( String path: sourcePath )
    {
      int i = path.lastIndexOf( "/src/main/java".replace( '/', File.separatorChar ) );
      if( i >= 0 )
      {
        resourcePath = path.substring( 0, i ) + "/src/main/resources".replace( '/', File.separatorChar );
        break;
      }
    }
    if( resourcePath != null && new File( resourcePath ).isDirectory() )
    {
      sourcePath.add( resourcePath );
    }
  }

  private void deriveSourcePath( Set<Pair<String, JavaFileObject>> inputFiles, Set<String> sourcePath )
  {
    outer:
    for( Pair<String, JavaFileObject> inputFile : inputFiles )
    {
      if( !isPhysicalFile( inputFile.getSecond() ) )
      {
        continue;
      }

      for( String sp : sourcePath )
      {
        if( inputFile.getSecond().getName().startsWith( sp + File.separatorChar ) )
        {
          continue outer;
        }
      }
      String type = inputFile.getFirst();
      if( type != null )
      {
        String path = derivePath( type, inputFile.getSecond() );
        sourcePath.add( path );
      }
      else
      {
        //noinspection unchecked
        getIssueReporter().report( new JavacDiagnostic( null, Diagnostic.Kind.WARNING, 0, 0, 0, IssueMsg.MSG_COULD_NOT_FIND_TYPE_FOR_FILE.get( inputFile ) ) );
      }
    }
  }

  private boolean isPhysicalFile( JavaFileObject inputFile )
  {
    URI uri = inputFile.toUri();
    return uri != null && uri.getScheme() != null && uri.getScheme().equalsIgnoreCase( "file" );
  }

  private String derivePath( String type, JavaFileObject inputFile )
  {
    String filename = inputFile.getName();
    int iDot = filename.lastIndexOf( '.' );
    String ext = iDot > 0 ? filename.substring( iDot ) : "";
    String pathRelativeFile = type.replace( '.', File.separatorChar ) + ext;
    assert filename.endsWith( pathRelativeFile );
    return filename.substring( 0, filename.indexOf( pathRelativeFile ) - 1 );
  }

  private List<String> fetchGosuInputFiles()
  {
    String property = System.getProperty( GOSU_SOURCE_FILES, "" );
    if( !property.isEmpty() )
    {
      return Arrays.asList( property.split( " " ) );
    }
    return Collections.emptyList();
  }

  @Override
  public void started( TaskEvent e )
  {
    switch( e.getKind() )
    {
      case ENTER:
        if( !_initialized )
        {
          _initialized = true;
          ManifoldHost.initializeAndCompileNonJavaFiles( _fileManager, _gosuInputFiles, this::deriveSourcePath, this::deriveClasspath, this::deriveOutputPath );
        }
        break;
    }
  }

  @Override
  public void finished( TaskEvent e )
  {
    switch( e.getKind() )
    {
      case PARSE:
      {
        addInputFile( e );
        break;
      }

      case ENTER:
        process( e );
        break;

    }
  }

  private void addInputFile( TaskEvent e )
  {
    if( !_initialized )
    {
      CompilationUnitTree compilationUnit = e.getCompilationUnit();
      ExpressionTree pkg = compilationUnit.getPackageName();
      String packageQualifier = pkg == null ? "" : (pkg.toString() + '.');
      for( Tree classDecl : compilationUnit.getTypeDecls() )
      {
        if( classDecl instanceof JCTree.JCClassDecl )
        {
          _javaInputFiles.add( new Pair<>( packageQualifier + ((JCTree.JCClassDecl)classDecl).getSimpleName(), compilationUnit.getSourceFile() ) );
        }
      }
    }
  }

  private void process( TaskEvent e )
  {
    Set<String> typesToProcess = new HashSet<>();
    ExpressionTree pkg = e.getCompilationUnit().getPackageName();
    String packageQualifier = pkg == null ? "" : (pkg.toString() + '.');
    for( Tree classDecl : e.getCompilationUnit().getTypeDecls() )
    {
      if( classDecl instanceof JCTree.JCClassDecl )
      {
        typesToProcess.add( packageQualifier + ((JCTree.JCClassDecl)classDecl).getSimpleName() );
        insertBootstrap( (JCTree.JCClassDecl)classDecl );
      }
    }
    _typeProcessor.addTypesToProcess( typesToProcess );
  }

  private void insertBootstrap( JCTree.JCClassDecl tree )
  {
    TreeTranslator visitor = new BootstrapInserter( this );
    tree.accept( visitor );
  }

  // Disable Java 9 warnings re "An illegal reflective access operation has occurred"
  private static void disableJava9IllegalAccessWarning()
  {
    // runtime
    disableJava9IllegalAccessWarning( JavacPlugin.class.getClassLoader() );
    // compile-time
    disableJava9IllegalAccessWarning( Thread.currentThread().getContextClassLoader() );
  }
  private static void disableJava9IllegalAccessWarning( ClassLoader cl )
  {
    try
    {
      Field theUnsafe = Unsafe.class.getDeclaredField( "theUnsafe" );
      theUnsafe.setAccessible( true );
      Unsafe u = (Unsafe)theUnsafe.get( null );

      Class cls = Class.forName( "jdk.internal.module.IllegalAccessLogger", false, cl );
      Field logger = cls.getDeclaredField( "logger" );
      u.putObjectVolatile( cls, u.staticFieldOffset( logger ), null );
    }
    catch( Throwable e )
    {
      // ignore
    }
  }
}
