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
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
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
import java.util.Objects;
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
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.util.JreUtil;
import manifold.rt.api.util.Pair;
import manifold.api.util.SourcePathUtil;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * A tool for parsing and compiling Java source.
 * <p>
 * A notable feature of this tool is its ability to compile Java sources that reference
 * and invoke Manifold types.  This feature enables bi-directional Java interop with Manifold.
 */
public class JavaParser implements IJavaParser
{
  private final IManifoldHost _host;
  private JavaCompiler _javac;
  private JavaFileManager _fileManager;
  private ManifoldJavaFileManager _mfm;
  private LocklessLazyVar<JavaCompiler> _parserJavac;

  /**
   * For internal use only.  Usea {@link IManifoldHost#getJavaParser()}
   */
  public JavaParser( IManifoldHost host )
  {
    _host = host;
    _parserJavac = LocklessLazyVar.make( JavacTool::create );
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
        _mfm = javacHook.getManifoldFileManager();
      }
      else
      {
        // Make a new Manifold file manager exclusively for this JavaParser

        _fileManager = _javac.getStandardFileManager( null, null, Charset.forName( "UTF-8" ) );

        try
        {
          IModule globalModule = getHost().getSingleModule();
          if( globalModule != null )
          {
            ((StandardJavaFileManager)_fileManager).setLocation( StandardLocation.SOURCE_PATH,
              globalModule.getSourcePath().stream()
                .map( IResource::toJavaFile )
                .filter( f -> !SourcePathUtil.excludeFromSourcePath( f.getAbsolutePath() ) )
                .collect( Collectors.toList() ) );
            ((StandardJavaFileManager)_fileManager).setLocation( StandardLocation.CLASS_PATH,
              globalModule.getJavaClassPath().stream()
                .map( IResource::toJavaFile )
                .filter( f -> !SourcePathUtil.excludeFromTestPath( f.getAbsolutePath() ) )
                .collect( Collectors.toList() ) );
          }
          _mfm = new ManifoldJavaFileManager( getHost(), _fileManager, null, false );
        }
        catch( IOException e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }

  public IManifoldHost getHost()
  {
    return _host;
  }

  public boolean parseType( String fqn, List<CompilationUnitTree> trees,
                            DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    Pair<JavaFileObject, String> pair = findJavaSource( fqn, errorHandler );
    if( pair == null )
    {
      return false;
    }

    StringWriter errors = new StringWriter();
    BasicJavacTask javacTask = (BasicJavacTask)_javac.getTask(
      errors, _mfm, errorHandler, Collections.singletonList( "-proc:none" ), null,
      Collections.singletonList( pair.getFirst() ) );
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

  public boolean parseText( String src, List<CompilationUnitTree> trees, Consumer<SourcePositions> sourcePositions,
                            Consumer<DocTrees> docTrees, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    ArrayList<JavaFileObject> javaStringObjects = new ArrayList<>();
    javaStringObjects.add( new StringJavaFileObject( "sample", src ) );
    StringWriter errors = new StringWriter();
    BasicJavacTask javacTask = (BasicJavacTask)_javac.getTask(
      errors, _mfm, errorHandler, Collections.singletonList( "-proc:none" ), null, javaStringObjects );
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

  public JCTree.JCExpression parseExpr( String expr, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    //!! Do not init() here; do not use _javac or _mfm for parseExpr() since this method is generally used
    //!! during the Parse phase, which happens earlier than the Enter phase where the plugin initializes much
    //!! of its state i.e., if parseExpr() is called before an Enter phase, it could use Manifold prematurely
    //!! and screw the pooch.  Also, we don't need Manifold for parsing expressions since it only produces a
    //!! simple AST with nothing resolved.
    // init();

    ArrayList<JavaFileObject> javaStringObjects = new ArrayList<>();
    String src =
      "class Sample {\n" +
      "  Object foo = " + expr + ";\n" +
      "}\n";
    javaStringObjects.add( new StringJavaFileObject( "sample", src ) );
    StringWriter errors = new StringWriter();
    BasicJavacTask javacTask = (BasicJavacTask)Objects.requireNonNull( _parserJavac.get() )
      .getTask( errors, null, errorHandler, Collections.singletonList( "-proc:none" ), null, javaStringObjects );
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
        List<? extends Tree> typeDecls = x.getTypeDecls();
        if( !typeDecls.isEmpty() )
        {
          JCTree.JCClassDecl tree = (JCTree.JCClassDecl)typeDecls.get( 0 );
          JCTree.JCVariableDecl field = (JCTree.JCVariableDecl)tree.getMembers().get( 0 );
          return field.getInitializer();
        }
      }
      return null;
    }
    catch( Exception e )
    {
      return null;
    }
  }

  /**
   * Compiles specified Java class name.  Maintains cache between calls to this method, therefore subsequent calls to this
   * method will consult the cache and return the previously compiled class if cached.
   */
  public InMemoryClassJavaFileObject compile( String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    InMemoryClassJavaFileObject compiledClass = _mfm.findCompiledFile( fqn );
    if( compiledClass != null )
    {
      return compiledClass;
    }

    Pair<JavaFileObject, String> fileObj = findJavaSource( fqn, errorHandler );
    if( fileObj == null )
    {
      return null;
    }

    int check = _mfm.pushRuntimeMode();
    try
    {
      StringWriter errors = new StringWriter();
      BasicJavacTask javacTask = (BasicJavacTask)_javac.getTask(
        errors, _mfm, errorHandler, options, null, Collections.singletonList( fileObj.getFirst() ) );
      initTypeProcessing( javacTask, Collections.singleton( fqn ) );
      javacTask.call();
      return _mfm.findCompiledFile( fileObj.getSecond() );
    }
    finally
    {
      _mfm.popRuntimeMode( check );
    }
  }

  /**
   * Compiles fresh, no caching.  Intended for use with parser feedback tooling e.g., a Java editor.
   */
  public InMemoryClassJavaFileObject compile( JavaFileObject jfo, String fqn, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();


    int check = _mfm.pushRuntimeMode();
    try
    {
      StringWriter errors = new StringWriter();
      BasicJavacTask javacTask = (BasicJavacTask)_javac.getTask(
        errors, _mfm, errorHandler, options, null, Collections.singletonList( jfo ) );
      initTypeProcessing( javacTask, Collections.singleton( fqn ) );
      javacTask.call();
      return _mfm.findCompiledFile( fqn );
    }
    finally
    {
      _mfm.popRuntimeMode( check );
    }
  }

  /**
   * Compiles a collection of java source files, intended for use a command line compiler.
   */
  public Collection<InMemoryClassJavaFileObject> compile( Collection<JavaFileObject> files, Iterable<String> options, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    init();

    int check = _mfm.pushRuntimeMode();
    try
    {
      StringWriter errors = new StringWriter();
      BasicJavacTask javacTask = (BasicJavacTask)_javac.getTask( errors, _mfm, errorHandler, options, null, files );
      initTypeProcessing( javacTask, files.stream().map( this::getTypeForFile ).collect( Collectors.toSet() ) );
      javacTask.call();
      return _mfm.getCompiledFiles();
    }
    finally
    {
      _mfm.popRuntimeMode( check );
    }
  }

  private String getTypeForFile( JavaFileObject file )
  {
    URI uri = file.toUri();
    if( !uri.getScheme().equalsIgnoreCase( "file" ) )
    {
      return makeTypeName( file.getName() );
    }
    IFile iFile = getHost().getFileSystem().getIFile( new File( file.getName() ) );
    List<IDirectory> sourcePath = getHost().getSingleModule().getSourcePath();
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

  private void initTypeProcessing( BasicJavacTask javacTask, Set<String> types )
  {
    TypeProcessor typeProcessor = new TypeProcessor( getHost(), javacTask );
    typeProcessor.addTypesToProcess( types );
  }

  @Override
  public Pair<JavaFileObject, String> findJavaSource( String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    init();

    if( _mfm == null )
    {
      // short-circuit reentrancy during init()
      return null;
    }

    JavaFileObject fileObj = _mfm.getSourceFileForInput( StandardLocation.SOURCE_PATH, fqn, JavaFileObject.Kind.SOURCE, errorHandler );
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

  public BasicJavacTask getJavacTask()
  {
    init();

    StringWriter errors = new StringWriter();
    return (BasicJavacTask)_javac.getTask( errors, _mfm, null, Arrays.asList( "-proc:none", "-source", "8" ), null, null );
  }

  @Override
  public void clear()
  {
    _javac = null;
    _parserJavac.clear();
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
