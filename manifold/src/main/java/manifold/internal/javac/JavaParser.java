package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IResource;
import manifold.api.host.IModule;
import manifold.internal.host.Manifold;
import manifold.internal.host.ManifoldHost;
import manifold.util.JreUtil;
import manifold.util.Pair;

/**
 * A tool for parsing and compiling Java source.
 * <p>
 * A notable feature of this tool is its ability to compile Java sources that reference
 * and invoke Manifold types.  This feature enables bi-directional Java interop with Manifold.
 */
public class JavaParser implements IJavaParser
{
  private static final ThreadLocal<JavaParser> INSTANCE = new ThreadLocal<>();

  public static JavaParser instance()
  {
    if( INSTANCE.get() == null )
    {
      INSTANCE.set( new JavaParser() );
    }
    return INSTANCE.get();
  }

  private JavaCompiler _javac;
  private JavaFileManager _fileManager;
  private ManifoldJavaFileManager _gfm;

  private JavaParser()
  {
  }

  private void init()
  {
    if( _javac == null )
    {
      _javac = JavacTool.create();

      JavacPlugin javacHook = JavacPlugin.instance();
      if( javacHook != null && !JreUtil.isJava9Modular_compiler( javacHook.getContext() ) )
      {
        // Share our existing Manifold file manager from Javac

        _fileManager = javacHook.getJavaFileManager();
        _gfm = javacHook.getManifoldFileManager();
      }
      else
      {
        // Make a new Manifold file manager exclusively for this JavaParser

        _fileManager = _javac.getStandardFileManager( null, null, Charset.forName( "UTF-8" ) );

        try
        {
          IModule globalModule = ManifoldHost.getGlobalModule();
          if( globalModule != null )
          {
            ((StandardJavaFileManager)_fileManager).setLocation( StandardLocation.SOURCE_PATH, globalModule.getSourcePath().stream().map( IResource::toJavaFile ).filter( f -> Manifold.excludeFromSourcePath( f.getAbsolutePath() ) ).collect( Collectors.toList() ) );
            ((StandardJavaFileManager)_fileManager).setLocation( StandardLocation.CLASS_PATH, globalModule.getJavaClassPath().stream().map( IResource::toJavaFile )/*.filter( f -> Manifold.excludeFromSourcePath( f.getAbsolutePath() ) )*/.collect( Collectors.toList() ) );
          }
          _gfm = new ManifoldJavaFileManager( _fileManager, null, false );
        }
        catch( IOException e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }

  public boolean parseType( String fqn, List<CompilationUnitTree> trees, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    Pair<JavaFileObject, String> pair = findJavaSource( fqn, errorHandler );
    if( pair == null )
    {
      return false;
    }

    StringWriter errors = new StringWriter();
    JavacTask javacTask = (JavacTask)_javac.getTask( errors, _gfm, errorHandler, Collections.singletonList( "-proc:none" ), null, Collections.singletonList( pair.getFirst() ) );
    try
    {
      initTypeProcessing( javacTask, Collections.singleton( fqn ) );
      Iterable<? extends CompilationUnitTree> iterable = javacTask.parse();
      for( CompilationUnitTree x : iterable )
      {
        trees.add( x );
      }
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  public boolean parseText( String src, List<CompilationUnitTree> trees, Consumer<SourcePositions> sourcePositions, Consumer<DocTrees> docTrees, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    ArrayList<JavaFileObject> javaStringObjects = new ArrayList<>();
    javaStringObjects.add( new StringJavaFileObject( "sample", src ) );
    StringWriter errors = new StringWriter();
    JavacTask javacTask = (JavacTask)_javac.getTask( errors, _gfm, errorHandler, Collections.singletonList( "-proc:none" ), null, javaStringObjects );
    try
    {
      initTypeProcessing( javacTask, Collections.singleton( "sample" ) );
      Iterable<? extends CompilationUnitTree> iterable = javacTask.parse();
      if( errors.getBuffer().length() > 0 )
      {
        System.err.println( errors.getBuffer() );
      }
      for( CompilationUnitTree x : iterable )
      {
        trees.add( x );
      }
      if( sourcePositions != null )
      {
        sourcePositions.accept( Trees.instance( javacTask ).getSourcePositions() );
      }
      if( docTrees != null )
      {
        docTrees.accept( DocTrees.instance( javacTask ) );
      }
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  /**
   * Compiles specified Java class name.  Maintains cache between calls to this method, therefore subsequent calls to this
   * method will consult the cache and return the previously compiled class if cached.
   */
  public InMemoryClassJavaFileObject compile( String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    InMemoryClassJavaFileObject compiledClass = _gfm.findCompiledFile( fqn );
    if( compiledClass != null )
    {
      return compiledClass;
    }

    Pair<JavaFileObject, String> fileObj = findJavaSource( fqn, errorHandler );
    if( fileObj == null )
    {
      return null;
    }

    StringWriter errors = new StringWriter();
    JavacTaskImpl javacTask = (JavacTaskImpl)_javac.getTask( errors, _gfm, errorHandler, options, null, Collections.singletonList( fileObj.getFirst() ) );
    initTypeProcessing( javacTask, Collections.singleton( fqn ) );
    javacTask.call();
    return _gfm.findCompiledFile( fileObj.getSecond() );
  }

  /**
   * Compiles fresh, no caching.  Intended for use with parser feedback tooling e.g., a Java editor.
   */
  public InMemoryClassJavaFileObject compile( JavaFileObject jfo, String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    StringWriter errors = new StringWriter();
    JavacTaskImpl javacTask = (JavacTaskImpl)_javac.getTask( errors, _gfm, errorHandler, options, null, Collections.singletonList( jfo ) );
    initTypeProcessing( javacTask, Collections.singleton( fqn ) );
    javacTask.call();
    return _gfm.findCompiledFile( fqn );
  }

  /**
   * Compiles a collection of java source files, intended for use a command line compiler.
   */
  public Collection<InMemoryClassJavaFileObject> compile( Collection<JavaFileObject> files, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    StringWriter errors = new StringWriter();
    JavacTaskImpl javacTask = (JavacTaskImpl)_javac.getTask( errors, _gfm, errorHandler, options, null, files );
    initTypeProcessing( javacTask, files.stream().map( this::getTypeForFile ).collect( Collectors.toSet() ) );
    javacTask.call();
    return _gfm.getCompiledFiles();
  }

  private String getTypeForFile( JavaFileObject file )
  {
    URI uri = file.toUri();
    if( !uri.getScheme().equalsIgnoreCase( "file" ) )
    {
      return makeTypeName( file.getName() );
    }
    IFile iFile = ManifoldHost.getFileSystem().getIFile( new File( file.getName() ) );
    List<IDirectory> sourcePath = ManifoldHost.getGlobalModule().getSourcePath();
    for( IDirectory dir : sourcePath )
    {
      if( iFile.isDescendantOf( dir ) )
      {
        return makeTypeName( iFile.getName().substring( dir.getName().length() ) );
      }
    }
    throw new IllegalStateException( "Could not infer type name from: " + file.getName() );
  }

  private String makeTypeName( String path )
  {
    return path
      .replace( '/', '.' )
      .replace( File.separatorChar, '.' )
      .substring( 0, path.lastIndexOf( '.' ) );
  }

  private void initTypeProcessing( JavacTask javacTask, Set<String> types )
  {
    TypeProcessor typeProcessor = new TypeProcessor( javacTask );
    typeProcessor.addTypesToProcess( types );
  }

  @Override
  public Pair<JavaFileObject, String> findJavaSource( String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    init();

    if( _gfm == null )
    {
      // short-circuit reentrancy during init()
      return null;
    }

    JavaFileObject fileObj = _gfm.getSourceFileForInput( StandardLocation.SOURCE_PATH, fqn, JavaFileObject.Kind.SOURCE, errorHandler );
    if( fileObj == null )
    {
      int iDot = fqn.lastIndexOf( '.' );
      if( iDot > 0 )
      {
        String enclosingFqn = fqn.substring( 0, iDot );
        return findJavaSource( enclosingFqn, errorHandler );
      }
      return null;
    }
    else
    {
      return new Pair<>( fileObj, fqn );
    }
  }

  public JavacTaskImpl getJavacTask()
  {
    init();

    StringWriter errors = new StringWriter();
    return (JavacTaskImpl)_javac.getTask( errors, _gfm, null, Arrays.asList( "-proc:none", "-source", "8" ), null, null );
  }

  @Override
  public void clear()
  {
    _javac = null;
    try
    {
      if( _fileManager != null )
      {
        _fileManager.close();
      }
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
