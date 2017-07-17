package manifold.internal.javac;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import com.sun.tools.javac.main.JavaCompiler;
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
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import manifold.internal.host.ManifoldHost;


import static javax.lang.model.SourceVersion.RELEASE_8;

/**
 */
@SupportedSourceVersion(RELEASE_8)
@SupportedAnnotationTypes({"*"})
public class JavacHook extends AbstractProcessor
{
  private static JavacHook INSTANCE = null;

  private static final String GOSU_SOURCE_FILES = "gosu.source.files";

  private JavacProcessingEnvironment _jpe;
  private Context _ctx;
  private ManifoldJavaFileManager _manFileManager;
  private JavaFileManager _fileManager;
  private Set<JavaFileObject> _javaInputFiles;
  private List<String> _gosuInputFiles;
  private TreeMaker _treeMaker;
  private JavacElements _javacElements;
  private TypeProcessor _typeProcessor;

  public static JavacHook instance()
  {
    return INSTANCE;
  }

  public JavacHook()
  {
    INSTANCE = this;
  }

  @Override
  public synchronized void init( ProcessingEnvironment processingEnv )
  {
    super.init( processingEnv );
    _jpe = (JavacProcessingEnvironment)processingEnv;
    _ctx = _jpe.getContext();
    _fileManager = _ctx.get( JavaFileManager.class );
    _javaInputFiles = fetchJavaInputFiles();
    _gosuInputFiles = fetchGosuInputFiles();
    _treeMaker = TreeMaker.instance( _ctx );
    _javacElements = JavacElements.instance( _ctx );
    _typeProcessor = new TypeProcessor( JavacTask.instance( _jpe ) );
    hijackJavacFileManager();
  }

  ManifoldJavaFileManager getManFileManager()
  {
    return _manFileManager;
  }

  JavaFileManager getJavaFileManager()
  {
    return _fileManager;
  }

  Context getContext()
  {
    return _ctx;
  }

  public JavacTask getJavacTask()
  {
    return JavacTask.instance( processingEnv );
  }

  TreeMaker getTreeMaker()
  {
    return _treeMaker;
  }

  JavacElements getJavacElements()
  {
    return _javacElements;
  }

  private void hijackJavacFileManager()
  {
    if( !(_fileManager instanceof ManifoldJavaFileManager) )
    {
      injectManFileManager();
      ManifoldHost.initializeAndCompileNonJavaFiles( _jpe, _fileManager, _gosuInputFiles, this::deriveSourcePath, this::deriveClasspath, this::deriveOutputPath );
    }
  }

  private void injectManFileManager()
  {
    _manFileManager = new ManifoldJavaFileManager( _fileManager, Log.instance( _ctx ), true );
    _ctx.put( JavaFileManager.class, (JavaFileManager)null );
    _ctx.put( JavaFileManager.class, _manFileManager );
  }

  private String deriveOutputPath()
  {
    try
    {
      String ping = "__dummy__";
      //JavaFileObject classFile = _jpe.getFiler().createClassFile( ping );
      JavaFileObject classFile = _fileManager.getJavaFileForOutput( StandardLocation.CLASS_OUTPUT, ping, JavaFileObject.Kind.CLASS, null );
      URI uri = classFile.toUri();
      if( !uri.getScheme().equals( "file" ) )
      {
        return "";
      }

      File dummyFile = new File( uri );
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
    URL[] classpathUrls = ((URLClassLoader)_jpe.getProcessorClassLoader()).getURLs();
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
        if( p != unmodifiedPath && p.startsWith( path ) )
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
    return sourcePath;
  }

  private void deriveSourcePath( Set<JavaFileObject> inputFiles, Set<String> sourcePath )
  {
    outer:
    for( JavaFileObject inputFile : inputFiles )
    {
      for( String sp : sourcePath )
      {
        if( inputFile.getName().startsWith( sp + File.separatorChar ) )
        {
          continue outer;
        }
      }
      TypeElement type = findType( inputFile );
      if( type != null )
      {
        String path = derivePath( type, inputFile );
        sourcePath.add( path );
      }
      else
      {
        _jpe.getMessager().printMessage( Diagnostic.Kind.WARNING, "Could not find type for file: " + inputFile );
      }
    }
  }

  private String derivePath( TypeElement type, JavaFileObject inputFile )
  {
    String filename = inputFile.getName();
    int iDot = filename.lastIndexOf( '.' );
    String ext = iDot > 0 ? filename.substring( iDot ) : "";
    String pathRelativeFile = type.getQualifiedName().toString().replace( '.', File.separatorChar ) + ext;
    assert filename.endsWith( pathRelativeFile );
    return filename.substring( 0, filename.indexOf( pathRelativeFile ) - 1 );
  }

  private TypeElement findType( JavaFileObject inputFile )
  {
    String path = inputFile.getName();
    int dotJava = path.lastIndexOf( ".java" );
    if( dotJava < 0 )
    {
      return null;
    }
    path = path.substring( 0, dotJava );
    List<String> tokens = new ArrayList<>();
    for( StringTokenizer tokenizer = new StringTokenizer( path, File.separator, false ); tokenizer.hasMoreTokens(); )
    {
      tokens.add( tokenizer.nextToken() );
    }
    String typeName = "";
    TypeElement type = null;
    for( int i = tokens.size() - 1; i >= 0; i-- )
    {
      typeName = tokens.get( i ) + typeName;
      TypeElement csr = getType( typeName );
      if( csr != null )
      {
        type = csr;
      }
      if( i > 0 )
      {
        typeName = '.' + typeName;
      }
    }
    return type;
  }

  private Set<JavaFileObject> fetchJavaInputFiles()
  {
    try
    {
      JavaCompiler javaCompiler = JavaCompiler.instance( _ctx );
      Field field = javaCompiler.getClass().getDeclaredField( "inputFiles" );
      field.setAccessible( true );
      //noinspection unchecked
      return (Set<JavaFileObject>)field.get( javaCompiler );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
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

  private TypeElement getType( String className )
  {
    return _jpe.getElementUtils().getTypeElement( className );
    // DeclaredType declaredType = jpe.getTypeUtils().getDeclaredType( typeElement );
    // return new ElementTypePair( typeElement, declaredType );
  }

  @Override
  public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv )
  {
    _typeProcessor.addTypesToProcess( roundEnv );

    if( roundEnv.processingOver() )
    {
      return false;
    }

    insertBootstrap( roundEnv );
    return false;
  }

  private void insertBootstrap( RoundEnvironment roundEnv )
  {
    Trees trees = Trees.instance( _jpe );

    Set<? extends Element> elements = roundEnv.getRootElements();
    for( Element elem : elements )
    {
      if( elem.getKind() == ElementKind.CLASS )
      {
        JCTree tree = (JCTree)trees.getTree( elem );
        TreeTranslator visitor = new BootstrapInserter( this );
        tree.accept( visitor );
      }
    }
  }
}
