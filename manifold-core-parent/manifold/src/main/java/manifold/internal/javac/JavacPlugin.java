/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.RichDiagnosticFormatter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.PathCache;
import manifold.api.fs.def.FileFragmentImpl;
import manifold.api.type.ICompilerComponent;
import manifold.api.type.ITypeManifold;
import manifold.api.util.JavacUtil;
import manifold.internal.host.JavacManifoldHost;
import manifold.internal.runtime.Bootstrap;
import manifold.api.util.IssueMsg;
import manifold.api.util.JavacDiagnostic;
import manifold.util.JreUtil;
import manifold.rt.api.util.ManClassUtil;
import manifold.util.NecessaryEvilUtil;
import manifold.rt.api.util.Pair;
import manifold.util.ReflectUtil;
import manifold.rt.api.util.StreamUtil;


import static manifold.api.type.ContributorKind.Supplemental;

/**
 */
public class JavacPlugin implements Plugin, TaskListener
{
  /** dynamic compilation mode */
  public static final String ARG_DYNAMIC = "dynamic";
  /** disables &lt;clinit&gt; bootstap */
  public static final String ARG_NO_BOOTSTRAP = "no-bootstrap";
  /** all plugin args */
  public static final String[] ARGS =
  {
    ARG_NO_BOOTSTRAP,
    ARG_DYNAMIC,
  };

  /** javac command line arguments for static compilation */
  private static final String MANIFOLD_SOURCE = "manifold.source";
  private static final String MANIFOLD_SOURCE_MAPPING = MANIFOLD_SOURCE + '.';
  private static final String OTHER_SOURCE_FILES = "other.source.files";
  private static final String OTHER_SOURCE_LIST = "other.source.list";

  private static Class<?> CLASSFINDER_CLASS = null;
  private static Class<?> MODULES_CLASS = null;
  private static Class<?> MODULEFINDER_CLASS = null;

  static
  {
    try
    {
      NecessaryEvilUtil.bypassJava9Security();
      // ClassFinder is new in Java 9, its presence indicates Java 9 or later
      CLASSFINDER_CLASS = Class.forName( "com.sun.tools.javac.code.ClassFinder", false, ClassReader.class.getClassLoader() );
      MODULES_CLASS = Class.forName( "com.sun.tools.javac.comp.Modules", false, ClassReader.class.getClassLoader() );
      MODULEFINDER_CLASS = Class.forName( "com.sun.tools.javac.code.ModuleFinder", false, ClassReader.class.getClassLoader() );
    }
    catch( Throwable ignore )
    {
    }
    loadJavacParserClass();
  }

  private static JavacPlugin INSTANCE;

  private JavacManifoldHost _host;
  private Context _ctx;
  private JavaFileManager _fileManager;
  private BasicJavacTask _javacTask;
  private Set<Pair<String, JavaFileObject>> _javaInputFiles;
  private Map<String, String> _otherSourceMappings;
  private List<String> _otherInputFiles;
  private TypeProcessor _typeProcessor;
  private IssueReporter _issueReporter;
  private ManifoldJavaFileManager _manFileManager;
  private boolean _initialized;
  private Map<Context, Set<Symbol>> _seenModules;
  private Map<String, Boolean> _argPresent;
  private ArrayList<FileFragmentResource> _fileFragmentResources;
  private Set<String> _javaSourcePath;
  private List<String> _manifoldSourcePath;
  private String _bootclasspath;
  private boolean _isIncremental;

  public static JavacPlugin instance()
  {
    return INSTANCE;
  }

  public JavacPlugin()
  {
  }

  @Override
  public String getName()
  {
    return "Manifold";
  }

  @Override
  public void init( JavacTask task, String... args )
  {
    INSTANCE = this;

    // calling this here because the line below this references the type `BasicJavacTask`, which is in a jdk module needing exposure
    NecessaryEvilUtil.bypassJava9Security();

    _javacTask = (BasicJavacTask)task;

    JavacProcessingEnvironment jpe = JavacProcessingEnvironment.instance( _javacTask.getContext() );

    processArgs( jpe, args );

    if( JreUtil.isJava16orLater() )
    {
      NecessaryEvilUtil.openModule( getContext(), "jdk.compiler" );
    }

    _host = new JavacManifoldHost();
    _fileFragmentResources = new ArrayList<>();
    _javaSourcePath = Collections.emptySet();
    assignBootclasspath();
    hijackJavacFileManager();
    overrideJavacToolEnter();
    task.addTaskListener( this );
  }

  private void overrideJavacToolEnter()
  {
    String JavadocTool_class =
      JreUtil.isJava8() ? "com.sun.tools.javadoc.JavadocTool" : "jdk.javadoc.internal.tool.JavadocTool";
    JavaCompiler javadocTool = (JavaCompiler)ReflectUtil.method( JavadocTool_class, "instance", Context.class )
      .invokeStatic( getContext() );
    if( javadocTool != null && javadocTool.getClass().getSimpleName().equals( "JavadocTool" ) )
    {
      // Ensure the JavacPlugin.initialize() method is called immediately before Javadoc Enters the first compilation unit
      Object manJavadocEnter = ReflectUtil.method( "manifold.internal.javac.ManJavadocEnter_" + (JreUtil.isJava8() ? 8 : 9),
        "instance", Context.class ).invokeStatic( getContext() );
      ReflectUtil.field( javadocTool, "javadocEnter" ).set( manJavadocEnter );
    }
  }

  private void assignBootclasspath()
  {
    try
    {
      String[] args = (String[]) ReflectUtil.field( _javacTask, "args" ).get();
      boolean found = false;
      for( String arg: args )
      {
        if( arg != null && arg.equalsIgnoreCase( "-bootclasspath" ) )
        {
          found = true;
        }
        else if( found )
        {
          _bootclasspath = arg;
          break;
        }
      }
    }
    catch( Exception ignore ) {}
  }

  private void processArgs( JavacProcessingEnvironment jpe, String[] args )
  {
    _argPresent = new HashMap<>();
    Arrays.stream( ARGS ).forEach( arg -> _argPresent.put( arg, testForArg( arg, args ) ) );
    notifyOfInvalidArgs( args, jpe );
  }

  public JavacManifoldHost getHost()
  {
    return _host;
  }

  private void notifyOfInvalidArgs( String[] args, JavacProcessingEnvironment jpe )
  {
    for( String arg: args )
    {
      if( Arrays.stream( ARGS ).noneMatch( validArg -> validArg.equals( arg ) ) )
      {
        jpe.getMessager().printMessage( Diagnostic.Kind.ERROR, "Unrecognized Manifold plugin argument '" + arg + "'" );
      }
    }
  }

  protected boolean testForArg( String name, String[] args )
  {
    boolean isPresent = isArgPresent( name, args );

    if( !isPresent )
    {
      // maven doesn't like the -Xplugin:"Manifold strings", it doesn't parse "Manifold string" as plugin name and argument, so we do it here:
      try
      {
        String[] rawArgs = (String[])ReflectUtil.field( _javacTask, "args" ).get();
        isPresent = Arrays.stream( rawArgs ).anyMatch( arg -> arg.contains( "-Xplugin:" ) && arg.contains( "Manifold" ) && arg.contains( name ) );
      }
      catch( Exception ignore )
      {
      }
    }
    return isPresent;
  }

  private boolean isArgPresent( String name, String[] args )
  {
    if( args == null )
    {
      return false;
    }
    for( String arg : args )
    {
      if( arg != null && arg.equalsIgnoreCase( name ) )
      {
        return true;
      }
    }
    return false;
  }

  public Context getContext()
  {
    return _javacTask.getContext();
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

  public BasicJavacTask getJavacTask()
  {
    return _javacTask;
  }

  @SuppressWarnings("unused")
  public Set<Pair<String, JavaFileObject>> getJavaInputFiles()
  {
    return _javaInputFiles;
  }

  public Map<String, String> getOtherSourceMappings()
  {
    return _otherSourceMappings;
  }

  @SuppressWarnings("unused")
  public List<String> getOtherInputFiles()
  {
    return _otherInputFiles;
  }

  @SuppressWarnings("WeakerAccess")
  public TreeMaker getTreeMaker()
  {
    return TreeMaker.instance( getContext() );
  }

  @SuppressWarnings("WeakerAccess")
  public JavacElements getJavacElements()
  {
    return JavacElements.instance( getContext() );
  }

  @SuppressWarnings("unused")
  public TypeProcessor getTypeProcessor()
  {
    return _typeProcessor;
  }

  public IssueReporter getIssueReporter()
  {
    return _issueReporter;
  }

  @SuppressWarnings("unused")
  public Set<String> getJavaSourcePath()
  {
    return _javaSourcePath;
  }

  public String getBootclasspath()
  {
    return _bootclasspath;
  }

  private void hijackJavacFileManager()
  {
    if( !(_fileManager instanceof ManifoldJavaFileManager) && _manFileManager == null )
    {
      _ctx = _javacTask.getContext();
      _fileManager = getContext().get( JavaFileManager.class );
      _javaInputFiles = new HashSet<>();
      _manifoldSourcePath = fetchManifoldSource();
      _otherInputFiles = fetchOtherInputFiles();
      _otherSourceMappings = fetchManifoldSourceMappings();
      _typeProcessor = new TypeProcessor( getHost(), _javacTask );
      _issueReporter = new IssueReporter( _javacTask::getContext );
      _seenModules = new HashMap<>();
      injectManFileManager();
    }
  }

  private void tailorJavaCompiler( TaskEvent te )
  {
    CompilationUnitTree compilationUnit = te.getCompilationUnit();
    if( !(compilationUnit instanceof JCTree.JCCompilationUnit) )
    {
      return;
    }

    // For type processing (##todo is this still necessary?)
    JavaCompiler compiler = JavaCompiler.instance( getContext() );
    compiler.shouldStopPolicyIfNoError = CompileStates.CompileState.max(
      compiler.shouldStopPolicyIfNoError, CompileStates.CompileState.FLOW );

    //
    // Both Java 8 and Java 9 alterations
    //

    // Override javac's Log for error suppression (@Jailbreak too, but that's only if extensions are enabled, see below)
    ReflectUtil.method( "manifold.internal.javac.ManLog_" + (JreUtil.isJava8() ? 8 : 9),
      "instance", Context.class ).invokeStatic( getContext() );

    // Override javac's ClassWriter
    ManClassWriter.instance( getContext() );

    // Override javac's Check
    ManCheck.instance( getContext() );

    if( !isExtensionsEnabled() )
    {
      // No need to hook up all the extension stuff if it's not enabled
      return;
    }

    // Override javac's Attr
    Attr manAttr = (Attr)ReflectUtil.method( "manifold.internal.javac.ManAttr_" + (JreUtil.isJava8() ? 8 : 9),
      "instance", Context.class ).invokeStatic( getContext() );

    // Override javac's Resolve
    ManResolve.instance( _ctx );

    // Override javac's TransTypes
    ManTransTypes.instance( _ctx );

    // Override javac's Types
    ManTypes.instance( _ctx );

    ((Log)ReflectUtil.field( manAttr, "log" ).get()).setDiagnosticFormatter( RichDiagnosticFormatter.instance( _ctx ) );

    if( !JreUtil.isJava8() )
    {
      //
      // Java 9+ specific alterations
      //

      Symbol module = (Symbol)ReflectUtil.field( compilationUnit, "modle" ).get();
      if( module == null )
      {
        return;
      }
      Set<Symbol> modules = _seenModules.computeIfAbsent( getContext(), k -> new LinkedHashSet<>() );
      if( modules.contains( module ) )
      {
        return;
      }

      modules.add( module );

      NecessaryEvilUtil.openModule( getContext(), "jdk.compiler" );

      if( JavacUtil.getSourceNumber() > 8 ) // don't override if -source 8
      {
        // Override javac's ClassFinder
        ReflectUtil.method( "manifold.internal.javac.ManClassFinder_9", "instance", Context.class )
          .invokeStatic( getContext() );
      }
    }
    notifyCompilerComponents();
  }

  private void notifyCompilerComponents()
  {
    for( ICompilerComponent cc: JavacPlugin.instance().getTypeProcessor().getCompilerComponents() )
    {
      cc.tailorCompiler();
    }
  }

  public boolean isExtensionsEnabled()
  {
    try
    {
      Class.forName( "manifold.ext.rt.api.Extension" );
      return true;
    }
    catch( ClassNotFoundException e )
    {
      return false;
    }
  }

  private void injectManFileManager()
  {
    // Override javac's JavaFileManager
    _fileManager = getContext().get( JavaFileManager.class );
    _manFileManager = new ManifoldJavaFileManager( getHost(), _fileManager, getContext(), true );
    getContext().put( JavaFileManager.class, (JavaFileManager)null );
    getContext().put( JavaFileManager.class, _manFileManager );

    // Assign our file manager to javac's various components
    try
    {
      if( JreUtil.isJava8() )
      {
        ReflectUtil.field( ClassReader.instance( getContext() ), "fileManager" ).set( _manFileManager );
      }
      else
      {
        Object classFinder = ReflectUtil.method( CLASSFINDER_CLASS, "instance", Context.class ).invokeStatic( getContext() );
        ReflectUtil.field( classFinder, "fileManager" ).set( _manFileManager );
        ReflectUtil.field( classFinder, "preferSource" ).set( true );

        Object modules = ReflectUtil.method( MODULES_CLASS, "instance", Context.class ).invokeStatic( getContext() );
        ReflectUtil.field( modules, "fileManager" ).set( _manFileManager );

        Object moduleFinder = ReflectUtil.method( MODULEFINDER_CLASS, "instance", Context.class ).invokeStatic( getContext() );
        ReflectUtil.field( moduleFinder, "fileManager" ).set( _manFileManager );
      }

      // Hack for using "-source 8" with Java 9
      try
      {
        Object classFinder = ReflectUtil.method( CLASSFINDER_CLASS, "instance", Context.class ).invokeStatic( getContext() );
        ReflectUtil.field( classFinder, "fileManager" ).set( _manFileManager );

        Object modules = ReflectUtil.method( MODULES_CLASS, "instance", Context.class ).invokeStatic( getContext() );
        ReflectUtil.field( modules, "fileManager" ).set( _manFileManager );

        Object moduleFinder = ReflectUtil.method( MODULEFINDER_CLASS, "instance", Context.class ).invokeStatic( getContext() );
        ReflectUtil.field( moduleFinder, "fileManager" ).set( _manFileManager );
      }
      catch( Throwable ignore )
      {
      }

      ReflectUtil.field( ClassWriter.instance( getContext() ), "fileManager" ).set( _manFileManager );
      ReflectUtil.field( Enter.instance( getContext() ), "fileManager" ).set( _manFileManager );
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
    if( JreUtil.isJava9Modular_compiler( getContext() ) )
    {
      List<String> pathsFromModules = new ArrayList<>();
      Object modulesUtil = ReflectUtil.method( "com.sun.tools.javac.comp.Modules", "instance", Context.class ).invokeStatic( getContext() );
      // an explicit is compiling, determine the class path from its dependencies, which are allModules visible via Modules util
      //noinspection unchecked
      for( Symbol m : (Iterable<Symbol>)ReflectUtil.method( modulesUtil, "allModules" ).invoke() )
      {
        Object classLocation = ReflectUtil.field( m, "classLocation" ).get();
        if( classLocation == null )
        {
          continue;
        }
        Collection<Path> paths;
        try
        {
          //noinspection unchecked
          paths = (Collection<Path>)ReflectUtil.method( classLocation, "getPaths" ).invoke();
        }
        catch( Exception e )
        {
          continue;
        }

        for( Path p : paths )
        {
          URI uri = p.toUri();
          String scheme = uri.getScheme();
          if( scheme.equalsIgnoreCase( "file" ) || scheme.equalsIgnoreCase( "jar" ) )
          {
            try
            {
              pathsFromModules.add( new File( uri ).getAbsolutePath() );
            }
            catch( IllegalArgumentException iae )
            {
              System.out.println( iae.getMessage() );
            }
          }
        }
      }
      return pathsFromModules;
    }

    ClassLoader cl = _javacTask.getContext().get( JavaFileManager.class ).getClassLoader( StandardLocation.CLASS_PATH );
    URL[] classpathUrls = getURLs( cl );
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

  private URL[] getURLs( ClassLoader cl )
  {
    if( cl instanceof URLClassLoader )
    {
      return ((URLClassLoader)cl).getURLs();
    }

    // intellij's LazyClassLoader
    ReflectUtil.LiveFieldRef myUrls = ReflectUtil.WithNull.field( cl, "myUrls" );
    if( myUrls != null )
    {
      Iterable<URL> urls = (Iterable<URL>)myUrls.get();
      return StreamSupport.stream( urls.spliterator(), false ).toArray( URL[]::new );
    }

    throw new UnsupportedOperationException( "Unhandled ClassLoader type: " + cl.getClass().getTypeName() );
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
    _javaSourcePath = new HashSet<>( sourcePath );
    deriveAdditionalSourcePath( _otherInputFiles, sourcePath );
    maybeAddResourcePath( _javaInputFiles, sourcePath );
    sourcePath.addAll( _manifoldSourcePath );
    return sourcePath;
  }

  private void deriveAdditionalSourcePath( List<String> inputFiles, Set<String> sourcePath )
  {
    outer:
    for( String inputFile : inputFiles )
    {
      for( String sp : sourcePath )
      {
        if( inputFile.startsWith( sp + File.separatorChar ) )
        {
          continue outer;
        }
      }
      String pkg = extractPackageName( inputFile );
      if( pkg != null )
      {
        int iDot = inputFile.lastIndexOf( '.' );
        String ext = iDot > 0 ? inputFile.substring( iDot ) : "";
        String fqn = pkg + '.' + new File( inputFile ).getName();
        fqn = fqn.substring( 0, fqn.length() - ext.length() );
        String path = derivePath( fqn, inputFile );
        sourcePath.add( path );
      }
      else
      {
        //noinspection unchecked
        getIssueReporter().report( new JavacDiagnostic( null, Diagnostic.Kind.WARNING, 0, 0, 0, IssueMsg.MSG_COULD_NOT_FIND_TYPE_FOR_FILE.get( inputFile ) ) );
      }
    }
  }

  /**
   * Add the 'resources' path from the conventional place.  This is not necessary because
   * resources are copied to the output directory, which is also part of the source path for
   * compilation.  However, having the resources directory in the path facilitates error
   * reporting in IDEs such as IntelliJ where there are hyper links directly to the errant
   * source file in the compilation results. Without the resources path, this link leads to
   * the output dir, which is confusing.
   * <p/>
   * Note incremental compilation from IntelliJ is another benefit of adding the resource path.
   * Our JPS plugin makes available the *resource files* that have changed.  Since we derive
   * the manifold types to compile from the resource files, we need to the 'resources' path,
   * otherwise we don't find any types and incremental compilation won't work.  Therefore,
   * we provide the resource paths definitively via a comment in the _Manifold_Temp_Main_.java
   * file provided by our JPS plugin.
   */
  private void maybeAddResourcePath( Set<Pair<String, JavaFileObject>> javaInputFiles, Set<String> sourcePath )
  {
    String resourcePath = null;
    for( String path : sourcePath )
    {
      int i = path.lastIndexOf( "/".replace( '/', File.separatorChar ) );
      if( i >= 0 )
      {
        resourcePath = path.substring( 0, i ) + "/resources".replace( '/', File.separatorChar );
        break;
      }
    }
    if( resourcePath != null && new File( resourcePath ).isDirectory() )
    {
      sourcePath.add( resourcePath );
    }

    // If compiling from IntelliJ, we provide the resource paths definitively via a comment in
    // the _Manifold_Temp_Main_.java file provided by our JPS plugin.
    deriveResourcePath( javaInputFiles, sourcePath );
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
        String path = derivePath( type, inputFile.getSecond().getName() );
        if( path != null )
        {
          sourcePath.add( path );
        }
      }
      else
      {
        //noinspection unchecked
        getIssueReporter().report( new JavacDiagnostic( null, Diagnostic.Kind.WARNING, 0, 0, 0, IssueMsg.MSG_COULD_NOT_FIND_TYPE_FOR_FILE.get( inputFile ) ) );
      }
    }
  }

  /**
   * Add resource paths more precisely via our JPS plugin in IntelliJ
   */
  private void deriveResourcePath( Set<Pair<String, JavaFileObject>> inputFiles, Set<String> resourcePath )
  {
    for( Pair<String, JavaFileObject> inputFile : inputFiles )
    {
      JavaFileObject fo = inputFile.getSecond();
      if( !isPhysicalFile( fo ) )
      {
        continue;
      }

      String filename = fo.getName();
      if( filename.contains( "_Manifold_Temp_Main_"  ) )
      {
        File file = new File( filename );
        if( file.isFile() )
        {
          addResourcePaths( file, resourcePath );
        }
      }
    }
  }

  private static final String RESOURCE_ROOTS = "//## ResourceRoots:";
  private void addResourcePaths( File file, Set<String> resourcePath )
  {
    try
    {
      String content = StreamUtil.getContent( new FileReader( file ) );
      int index = content.indexOf( RESOURCE_ROOTS );
      if( index >= 0 )
      {
        int iEol = content.indexOf( '\n', index );
        String paths = content.substring( index + RESOURCE_ROOTS.length(), iEol );
        paths = paths.trim();
        for( StringTokenizer tokenizer = new StringTokenizer( paths, File.pathSeparator );
             tokenizer.hasMoreTokens(); )
        {
          String path = tokenizer.nextToken();
          if( new File( path ).isDirectory() )
          {
            resourcePath.add( path );
          }
        }
      }
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isPhysicalFile( JavaFileObject inputFile )
  {
    URI uri = inputFile.toUri();
    return uri != null && uri.getScheme() != null && uri.getScheme().equalsIgnoreCase( "file" );
  }

  private String derivePath( String type, String sourceFile )
  {
    sourceFile = new File( sourceFile ).getAbsolutePath();
    int iDot = sourceFile.lastIndexOf( '.' );
    String ext = iDot > 0 ? sourceFile.substring( iDot ) : "";
    String pathRelativeFile = type.replace( '.', File.separatorChar ) + ext;
    assert sourceFile.endsWith( pathRelativeFile );
    int typeIndex = sourceFile.indexOf( pathRelativeFile );
    return typeIndex > 0 ? sourceFile.substring( 0, typeIndex - 1 ) : null;
  }

  private List<String> fetchManifoldSource()
  {
    Map<String, String> options = JavacProcessingEnvironment.instance( getContext() ).getOptions();
    String manifoldSourceProperty = getManifoldSourceProperty( options );
    List<String> dirs = Collections.emptyList();
    if( manifoldSourceProperty != null && !manifoldSourceProperty.isEmpty() )
    {
      dirs = Arrays.asList( manifoldSourceProperty.split( " " ) );
    }
    return dirs;
  }

  private List<String> fetchOtherInputFiles()
  {
    Map<String, String> options = JavacProcessingEnvironment.instance( getContext() ).getOptions();

    String otherSourceFiles = getOtherSourceFilesProperty( options );
    String otherSourceList = getOtherSourceListProperty( options );
    if( otherSourceFiles != null && otherSourceList != null )
    {
      throw new IllegalArgumentException( String.format( "Properties %s and %s may not be set simultaneously; please choose one or the other.", OTHER_SOURCE_FILES, OTHER_SOURCE_LIST ) );
    }

    List<String> files = Collections.emptyList();

    if( otherSourceFiles != null && !otherSourceFiles.isEmpty() )
    {
      files = Arrays.asList( otherSourceFiles.split( " " ) );
    }
    else if( otherSourceList != null && !otherSourceList.isEmpty() )
    {
      try
      {
        files = Files.readAllLines( new File( otherSourceList ).toPath() )
          .stream()
          .filter( s -> !s.isEmpty() )
          .collect( Collectors.toList() );
      }
      catch( IOException e )
      {
        throw new IllegalStateException( String.format( "Unable to read source list from %s", otherSourceList ), e );
      }
    }

    return files;
  }

  private Map<String, String> fetchManifoldSourceMappings()
  {
    Map<String, String> sourceMappings = new HashMap<>();
    Map<String, String> options = JavacProcessingEnvironment.instance( getContext() ).getOptions();
    for( Map.Entry<String, String> option: options.entrySet() )
    {
      String key = option.getKey();
      if( key.startsWith( MANIFOLD_SOURCE_MAPPING ) )
      {
        // class:tm-class-name -> type-name-regex
        // or
        // file-ext -> type-name-regex

        String fqnOrExt = key.substring( MANIFOLD_SOURCE_MAPPING.length() );
        sourceMappings.put( fqnOrExt, option.getValue() );
      }
    }
    return sourceMappings;
  }

  private String getManifoldSourceProperty( Map<String, String> options )
  {
    String manifoldSourceFiles = options.get( MANIFOLD_SOURCE );
    if( manifoldSourceFiles == null )
    {
      manifoldSourceFiles = System.getProperty( MANIFOLD_SOURCE );
    }
    return manifoldSourceFiles;
  }
  private String getOtherSourceFilesProperty( Map<String, String> options )
  {
    String otherSourceFiles = options.get( OTHER_SOURCE_FILES );
    if( otherSourceFiles == null )
    {
      otherSourceFiles = System.getProperty( OTHER_SOURCE_FILES );
    }
    return otherSourceFiles;
  }
  private String getOtherSourceListProperty( Map<String, String> options )
  {
    String otherSourceList = options.get( OTHER_SOURCE_LIST );
    if( otherSourceList == null )
    {
      otherSourceList = System.getProperty( OTHER_SOURCE_LIST );
    }
    return otherSourceList;
  }

  public void initialize( TaskEvent e )
  {
    if( !_initialized )
    {
      _initialized = true;

      // Must perform shenanigans early
      NecessaryEvilUtil.bypassJava9Security();

      // Initialize the Javac host environment
      getHost().initialize( deriveSourcePath(), deriveClasspath(), deriveOutputPath() );

      // Initialize the runtime host for dynamically loading darkj classes Manifold itself uses during compilation e.g., ManClassFinder_9
      Bootstrap.init();

      // Override javac's ClassFinder and Resolve so that we can safely load class symbols corresponding with extension classes
      tailorJavaCompiler( e );
    }
    else if( _javacTask.getContext() != _ctx )
    {
      // If annotation processors are present, javac creates a whole new JavaCompiler and ctx before Analyze phase...

      _ctx = _javacTask.getContext();

      // Override javac's stuff again for the new ctx
      tailorJavaCompiler( e );
      injectManFileManager();
    }
  }

  @Override
  public void started( TaskEvent e )
  {
    switch( e.getKind() )
    {
      case PARSE:
        // override the ParserFactory to support fragments in comments and string literals
        ManParserFactory parserFactory = ManParserFactory.instance( _javacTask.getContext() );
        parserFactory.setTaskEvent( e );
        ReflectUtil.field( JavaCompiler.instance( _javacTask.getContext() ), "parserFactory" ).set( parserFactory );
        break;

      case ENTER:
        initialize( e );
        // add the fragments created during parsing
        addFileFragments( e );
        break;

      case ANALYZE:
        initialize( e );
        // Add extension methods to javac's array type
        extendArrayType( e );
        break;
    }
  }

  public void extendArrayType( TaskEvent e )
  {
    try
    {
      ArrayTypeExtender.extend( getContext(), e.getCompilationUnit() );
    }
    catch( Exception ignore )
    {
      // although this method is invoked during the ANALYZE phase, the compiler's state may still be in the ENTER phase,
      // Java 9+ throws in this case (we ignore for now)
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
        processParse( e );
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

  private String extractPackageName( String file )
  {
    try
    {
      String source = StreamUtil.getContent( new FileReader( file ) );
      int iPkg = source.indexOf( "package" );
      if( iPkg >= 0 )
      {
        int iEol = source.indexOf( '\n', iPkg );
        if( iEol > iPkg )
        {
          String pkg = source.substring( iPkg + "package".length(), iEol ).trim();
          for( StringTokenizer tokenizer = new StringTokenizer( pkg, "." ); tokenizer.hasMoreTokens(); )
          {
            String part = tokenizer.nextToken();
            if( !ManClassUtil.isJavaIdentifier( part ) )
            {
              return null;
            }
          }
          return pkg;
        }
      }
      return null;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
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

  private void processParse( TaskEvent e )
  {
    for( Tree classDecl : e.getCompilationUnit().getTypeDecls() )
    {
      if( classDecl instanceof JCTree.JCClassDecl )
      {
        ((JCTree.JCClassDecl)classDecl).accept( new ParseProcessor( this ) );
      }
    }
  }

  private void insertBootstrap( JCTree.JCClassDecl tree )
  {
    // we construct BootstrapInserter reflectively because it extends TreeTranslator,
    // which we have yet to open/export as JavacPlugin is loaded before that time
    TreeTranslator visitor = (TreeTranslator)ReflectUtil
      .constructor( "manifold.internal.javac.BootstrapInserter", JavacPlugin.class ).newInstance( this );
    tree.accept( visitor );
  }

  public boolean isStaticCompile()
  {
    return !_argPresent.get( ARG_DYNAMIC );
  }

  public boolean isNoBootstrapping()
  {
    return _argPresent.get( ARG_NO_BOOTSTRAP );
  }

  public void registerType( JavaFileObject sourceFile, int offset, String name, String ext, HostKind hostKind, String content )
  {
    _fileFragmentResources.add( new FileFragmentResource( sourceFile, offset, name, ext, hostKind, content ) );
  }

  private void addFileFragments( TaskEvent e )
  {
    //noinspection Java8CollectionRemoveIf
    for( Iterator<FileFragmentResource> iterator = _fileFragmentResources.iterator(); iterator.hasNext(); )
    {
      FileFragmentResource fragment = iterator.next();
      if( fragment.embed( e ) )
      {
        iterator.remove();
      }
    }
  }

  public boolean isIncremental()
  {
    return _isIncremental || String.valueOf( true ).equals( System.getProperty( "manifold.compiler.incremental" ) );
  }
  public void setIncremental()
  {
    _isIncremental = true;
  }

  private class FileFragmentResource
  {
    private final JavaFileObject _sourceFile;
    private final int _offset;
    private final String _name;
    private final String _ext;
    private final HostKind _hostKind;
    private final String _content;

    private FileFragmentResource( JavaFileObject sourceFile, int offset, String name, String ext, HostKind hostKind, String content )
    {
      _sourceFile = sourceFile;
      _offset = offset;
      _name = name;
      _ext = ext;
      _hostKind = hostKind;
      _content = content;
    }

    private boolean embed( TaskEvent e )
    {
      JavaFileObject sourceFile = e.getSourceFile();
      if( !sourceFile.equals( _sourceFile ) )
      {
        return false;
      }

      IFile file;
      try
      {
        file = getHost().getFileSystem().getIFile( sourceFile.toUri().toURL() );
      }
      catch( Exception ex )
      {
        return false;
      }

      ExpressionTree pkg = e.getCompilationUnit().getPackageName();
      if( pkg == null )
      {
        return false;
      }

      FileFragmentImpl fragment =
        new FileFragmentImpl( _name, _ext, _hostKind, file, _offset, _content.length(), _content );
      JavacManifoldHost host = JavacPlugin.instance().getHost();
      Set<ITypeManifold> tms = host.getSingleModule()
        .findTypeManifoldsFor( fragment, t -> t.getContributorKind() != Supplemental );
      ITypeManifold tm = tms.stream().findFirst().orElse( null );
      if( tm == null )
      {
        //## todo: add compile warning
        return true;
      }

      // ensure path cache is created before creation notify
      host.getSingleModule().getPathCache();

      String fqn = PathCache.qualifyName( pkg.toString(), _name );
      host.createdType( fragment, new String[] {fqn} );
      return true;
    }
  }

  /*
   * Total hack to load our ManJavacParser, which subclasses Java's JavacParser, but also must override package-private
   * methods, so is loaded directly into the same classloader as JavacParser.
   */
  private static void loadJavacParserClass()
  {
    ClassLoader classLoader = JavacParser.class.getClassLoader();
    synchronized(
      ReflectUtil.method( classLoader, "getClassLoadingLock", String.class )
        .invoke( "com.sun.tools.javac.parser.ManJavacParser" ) )
    {
      if( null == ReflectUtil.method( classLoader, "findLoadedClass", String.class )
        .invoke( "com.sun.tools.javac.parser.ManJavacParser" ) )
      {
        InputStream is1 = JavacPlugin.class.getClassLoader().getResourceAsStream(
          "manifold/internal/javac/ManJavacParser.clazz" );
        try
        {
          byte[] content = StreamUtil.getContent( is1 );
          ReflectUtil.method( classLoader, "defineClass", String.class, byte[].class, int.class, int.class )
            .invoke( "com.sun.tools.javac.parser.ManJavacParser", content, 0, content.length );
        }
        catch( IOException e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }
}
