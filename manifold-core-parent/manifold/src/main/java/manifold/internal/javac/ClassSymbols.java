package manifold.internal.javac;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import manifold.api.fs.IResource;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.host.RefreshRequest;
import manifold.internal.host.ManifoldHost;
import manifold.util.ManClassUtil;
import manifold.util.Pair;

/**
 * Utility to get ClassSymbol for a given type name.
 * <p/>
 * Note this class must have a FileManager separate from the one used in JavaParser (or JavacPlugin)
 * to avoid circularity issues.
 */
public class ClassSymbols
{
  private static final Map<IModule, ClassSymbols> INSTANCES = new ConcurrentHashMap<>();

  private final IModule _module;
  private JavacTool _javacTool;
  private volatile StandardJavaFileManager _fm;

  public static ClassSymbols instance( IModule module )
  {
    ClassSymbols classSymbols = INSTANCES.get( module );
    if( classSymbols == null )
    {
      INSTANCES.put( module, classSymbols = new ClassSymbols( module ) );
    }
    return classSymbols;
  }

  private ClassSymbols( IModule module )
  {
    _module = module;
    ManifoldHost.addTypeLoaderListenerAsWeakRef( module, new CacheClearer() );
  }

  private void init()
  {
    if( _fm != null )
    {
      return;
    }

    synchronized( this )
    {
      if( _fm != null )
      {
        return;
      }

      _javacTool = JavacTool.create();
      StandardJavaFileManager fm = _javacTool.getStandardFileManager( null, null, Charset.forName( "UTF-8" ) );

      try
      {
        fm.setLocation( StandardLocation.SOURCE_PATH, _module.getCollectiveSourcePath().stream().map( IResource::toJavaFile ).collect( Collectors.toList() ) );
        fm.setLocation( StandardLocation.CLASS_PATH, _module.getCollectiveJavaClassPath().stream().map( IResource::toJavaFile ).collect( Collectors.toList() ) );
        _fm = fm;
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  public BasicJavacTask getJavacTask()
  {
    init();

    StringWriter errors = new StringWriter();
    BasicJavacTask task = (BasicJavacTask)_javacTool.getTask( errors, _fm, null, Arrays.asList( "-proc:none", "-source", "1.8", "-Xprefer:source" ), null, null );
    if( errors.getBuffer().length() > 0 )
    {
      // report errors to console
      System.err.println( errors.getBuffer() );
    }
    return task;
  }

  public Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> getClassSymbol( BasicJavacTask javacTask, String fqn )
  {
    JavacElements elementUtils = JavacElements.instance( javacTask.getContext() );
    Symbol.ClassSymbol typeElement = elementUtils.getTypeElement( fqn );

    if( typeElement == null )
    {
      // For the case where the class is generated from a type manifold esp. from a IExtensionClassProducer
      return getClassSymbolForProducedClass( fqn, new BasicJavacTask[1] );
    }

    JavacTrees trees = JavacTrees.instance( javacTask.getContext() );
    TreePath path = trees.getPath( typeElement );
    if( path != null )
    {
      return new Pair<>( typeElement, (JCTree.JCCompilationUnit)path.getCompilationUnit() );
    }
    else
    {
      // TreePath is only applicable to a source file;
      // if fqn is not a source file, there is no compilation unit available
      return new Pair<>( typeElement, null );
    }
  }

  public SrcClass makeSrcClassStub( String fqn )
  {
    return makeSrcClassStub( fqn, null, null );
  }

  public SrcClass makeSrcClassStub( String fqn, BasicJavacTask[] javacTaskOut, JCTree.JCCompilationUnit[] compUnit )
  {
    BasicJavacTask javacTask = javacTaskOut != null && javacTaskOut[0] != null ? javacTaskOut[0] : getJavacTask();
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> pair = getClassSymbol( javacTask, fqn );
    if( compUnit != null )
    {
      compUnit[0] = pair.getSecond();
    }
    if( javacTaskOut != null )
    {
      javacTaskOut[0] = javacTask;
    }

    Symbol.ClassSymbol classSymbol = pair.getFirst();
    if( classSymbol == null )
    {
      // For the case where the class is generated from a type manifold esp. from a IExtensionClassProducer
      return makeSrcClassStubFromProducedClass( fqn, compUnit );
    }

    return SrcClassUtil.instance().makeStub( _module, fqn, classSymbol, pair.getSecond(), javacTask );
  }

  private SrcClass makeSrcClassStubFromProducedClass( String fqn, JCTree.JCCompilationUnit[] compUnit )
  {
    BasicJavacTask[] task = new BasicJavacTask[1];
    final Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> pair = getClassSymbolForProducedClass( fqn, task );

    if( compUnit != null )
    {
      compUnit[0] = pair.getSecond();
    }

    Symbol.ClassSymbol classSymbol = pair.getFirst();

    return SrcClassUtil.instance().makeStub( _module, fqn, classSymbol, pair.getSecond(), task[0] );
  }

  private Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> getClassSymbolForProducedClass( String fqn, BasicJavacTask[] task )
  {
    init();

    Pair<JavaFileObject, String> fileObj = JavaParser.instance().findJavaSource( fqn, null );
    if( fileObj == null )
    {
      return null;
    }

    StringWriter errors = new StringWriter();
    task[0] = (BasicJavacTask)_javacTool.getTask( errors, _fm, null, Arrays.asList( "-proc:none", "-source", "1.8", "-Xprefer:source" ), null, Collections.singleton( fileObj.getFirst() ) );
    try
    {
      Iterable<? extends Element> elements = task[0].analyze();
      for( Element e: elements )
      {
        if( e instanceof Symbol.ClassSymbol && e.getSimpleName().contentEquals( ManClassUtil.getShortClassName( fqn ) ) )
        {
          JavacTrees trees = JavacTrees.instance( task[0].getContext() );
          TreePath path = trees.getPath( e );
          if( path != null )
          {
            return new Pair<>( (Symbol.ClassSymbol)e, (JCTree.JCCompilationUnit)path.getCompilationUnit() );
          }
          else
          {
            // TreePath is only applicable to a source file;
            // if fqn is not a source file, there is no compilation unit available
            return new Pair<>( (Symbol.ClassSymbol)e, null );
          }
        }
      }
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }

    StringBuffer errorText = errors.getBuffer();
    if( errorText.length() > 0  )
    {
      throw new RuntimeException( "Compile errors:\n" + errorText );
    }

    return null;
  }

  private class CacheClearer implements ITypeLoaderListener
  {
    @Override
    public void refreshedTypes( RefreshRequest request )
    {
    }

    @Override
    public void refreshed()
    {
      INSTANCES.remove( _module );
    }
  }
}
