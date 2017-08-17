package manifold.ext;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import junit.framework.TestCase;
import manifold.ExtIssueMsg;
import manifold.api.fs.IDirectory;
import manifold.internal.host.ManifoldHost;
import manifold.util.IssueMsg;
import manifold.util.Pair;
import manifold.util.StreamUtil;

/**
 */
public class CompilationTest extends TestCase
{
  public void testCompilation() throws ClassNotFoundException, IllegalAccessException, InstantiationException
  {
    List<IDirectory> sourcePath = new ArrayList<>( ManifoldHost.getGlobalModule().getSourcePath() );
    sourcePath.add( ManifoldHost.getFileSystem().getIDirectory( JavacTask.class.getProtectionDomain().getCodeSource().getLocation() ) );
    ClassLoader cl = new URLClassLoader( sourcePath.stream().map( e ->
                                                                  {
                                                                    try
                                                                    {
                                                                      return e.toURI().toURL();
                                                                    }
                                                                    catch( MalformedURLException e1 )
                                                                    {
                                                                      throw new RuntimeException( e1 );
                                                                    }
                                                                  } ).toArray( URL[]::new ), null );
    Class<?> cls = Class.forName( Tests.class.getName(), true, cl );
    cls.newInstance();
  }

  /**
   * We need to compile the test classes in a clean environment, hence the separate classloader here.
   */
  @SuppressWarnings("unused")
  public static class Tests
  {
    private JavacFileManager _fm;
    private JavacTool _javacTool;
    private File _outputDir;

    public Tests() throws IOException, URISyntaxException
    {
      testCompilation();
    }

    public void testCompilation() throws IOException, URISyntaxException
    {
      ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
      try
      {
        initCompiler();

        runTests();
      }
      finally
      {
        Thread.currentThread().setContextClassLoader( prevLoader );
        //noinspection ResultOfMethodCallIgnored
        _outputDir.delete();
      }
    }

    private void runTests() throws IOException, URISyntaxException
    {
      compileFile( _javacTool, _fm, "/extensions/abc/benis_png/MyBenis_pngExt_Test.java",
                   new Pair<>( ExtIssueMsg.MSG_EXTENSION_SHADOWS, 1 ),
                   new Pair<>( ExtIssueMsg.MSG_MAYBE_MISSING_THIS, 19 ) );
      compileFile( _javacTool, _fm, "/extensions/java/util/List/ListExt_Test.java",
                   new Pair<>( ExtIssueMsg.MSG_EXTENSION_SHADOWS, 1 ) );
      compileFile( _javacTool, _fm, "/extensions/java/util/List/ListExt2_Test.java",
                   new Pair<>( ExtIssueMsg.MSG_EXTENSION_SHADOWS, 1 ) );
      compileFile( _javacTool, _fm, "/extensions/java/util/List/ListExt3_Test.java",
                   new Pair<>( ExtIssueMsg.MSG_THIS_FIRST, 17 ),
                   new Pair<>( ExtIssueMsg.MSG_EXPECTING_TYPE_FOR_THIS, 20 ),
                   new Pair<>( ExtIssueMsg.MSG_MAYBE_MISSING_THIS, 23 ),
                   new Pair<>( ExtIssueMsg.MSG_MUST_BE_STATIC, 26 ),
                   new Pair<>( ExtIssueMsg.MSG_MUST_BE_STATIC, 29 ),
                   new Pair<>( ExtIssueMsg.MSG_MUST_NOT_BE_PRIVATE, 33 ) );
    }

    private void initCompiler() throws IOException
    {
      _javacTool = JavacTool.create();
      DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<>();
      _fm = _javacTool.getStandardFileManager( dc, Locale.getDefault(), Charset.defaultCharset() );
      URLClassLoader loader = (URLClassLoader)getClass().getClassLoader();
      List<File> classpath = Arrays.stream( loader.getURLs() ).map( url ->
                                                                    {
                                                                      try
                                                                      {
                                                                        return new File( url.toURI() );
                                                                      }
                                                                      catch( URISyntaxException e )
                                                                      {
                                                                        throw new RuntimeException( e );
                                                                      }
                                                                    } ).collect( Collectors.toList() );
      _fm.setLocation( StandardLocation.SOURCE_PATH, classpath );
      _fm.setLocation( StandardLocation.CLASS_PATH, classpath );

      _outputDir = File.createTempFile( "tmp", null, null );
      //noinspection ResultOfMethodCallIgnored
      _outputDir.delete();
      //noinspection ResultOfMethodCallIgnored
      _outputDir.mkdir();
      _outputDir.deleteOnExit();
      _fm.setLocation( StandardLocation.CLASS_OUTPUT, Collections.singletonList( _outputDir ) );
    }

    @SafeVarargs
    private final void compileFile( JavacTool javacTool, JavaFileManager fileManager, String file, Pair<IssueMsg, Integer>... msgs ) throws IOException, URISyntaxException
    {
      DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<>();
      String content = StreamUtil.getContent( StreamUtil.getInputStreamReader( getClass().getResourceAsStream( file ) ) );
      SourceFile srcFile = new SourceFile( file, content );
      StringWriter errors = new StringWriter();
      JavacTaskImpl javacTask = (JavacTaskImpl)javacTool.getTask( errors, fileManager, dc, Collections.singletonList( "-Xplugin:Manifold" ), null, Collections.singletonList( srcFile ) );
      javacTask.call();
      outer:
      for( Pair<IssueMsg, Integer> msg : msgs )
      {
        for( Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics() )
        {
          if( d.getLineNumber() == msg.getSecond() )
          {
            if( msg.getFirst().isMessageSimilar( d.getMessage( Locale.getDefault() ) ) )
            {
              continue outer;
            }
          }
        }
        fail( "Did not find issue: " + msg.getFirst().get() + " at line: " + msg.getSecond() );
      }
    }

    private static class SourceFile extends SimpleJavaFileObject
    {
      private final String _source;

      protected SourceFile( String fqn, String source ) throws URISyntaxException
      {
        super( new URI( fqn ), Kind.SOURCE );
        _source = source;
      }

      @Override
      public CharSequence getCharContent( boolean ignoreEncodingErrors ) throws IOException
      {
        return _source;
      }
    }
  }
}
