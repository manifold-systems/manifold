package manifold.internal.javac;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.tools.JavaFileManager;
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
import manifold.util.SourcePathUtil;
import manifold.util.concurrent.LocklessLazyVar;

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
  private LocklessLazyVar<BasicJavacTask> _altJavacTask;
  private JavacTool _javacTool;
  private volatile StandardJavaFileManager _fm;
  private JavaFileManager _wfm;

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
    _altJavacTask = LocklessLazyVar.make( () -> {
      init();

      StringWriter errors = new StringWriter();
      BasicJavacTask task = (BasicJavacTask)_javacTool.getTask( errors, _fm, null, Arrays.asList( "-proc:none", "-source", "1.8", "-Xprefer:source" ), null, null );
      if( errors.getBuffer().length() > 0 )
      {
        // report errors to console
        System.err.println( errors.getBuffer() );
      }
      return task;
    } );
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
        fm.setLocation( StandardLocation.SOURCE_PATH, _module.getCollectiveSourcePath().stream().map( IResource::toJavaFile ).filter( f -> !SourcePathUtil.excludeFromTestPath( f.getAbsolutePath() ) ).collect( Collectors.toList() ) );
        fm.setLocation( StandardLocation.CLASS_PATH, _module.getCollectiveJavaClassPath().stream().map( IResource::toJavaFile ).filter( f -> !SourcePathUtil.excludeFromTestPath( f.getAbsolutePath() ) ).collect( Collectors.toList() ) );
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
    return _altJavacTask.get();
  }

  public Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> getClassSymbol( BasicJavacTask javacTask, String fqn )
  {
    return getClassSymbol( javacTask, (TypeProcessor)null, fqn );
  }

  public Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> getClassSymbol( BasicJavacTask javacTask, JavaFileManager.Location location, String fqn )
  {
    return getClassSymbol( javacTask.getContext(), location, fqn );
  }

  public Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> getClassSymbol( BasicJavacTask javacTask, TypeProcessor tp, String fqn )
  {
    Context ctx = tp == null ? javacTask.getContext() : tp.getContext();
    JCTree.JCCompilationUnit cu = tp == null ? null : (JCTree.JCCompilationUnit)tp.getCompilationUnit();
    return getClassSymbol( ctx, cu, fqn );
  }

  private Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> getClassSymbol( Context ctx, Object moduleCtx, String fqn )
  {
    Symbol.ClassSymbol typeElement = IDynamicJdk.instance().getTypeElement( ctx, moduleCtx, fqn );
    if( typeElement == null )
    {
      // For the case where the class is generated from a type manifold esp. from a IExtensionClassProducer
      return getClassSymbolForProducedClass( fqn, new BasicJavacTask[1] );

//## want this instead, but the typeElement is not complete in this case, investigate this
//      if( JavacPlugin.instance() != null )
//      {
//        typeElement = IDynamicJdk.instance().getTypeElement( JavacPlugin.instance().getContext(), moduleCtx, fqn );
//        typeElement.complete();
//      }
    }

    JavacTrees trees = JavacTrees.instance( ctx );
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
    return makeSrcClassStub( fqn, null );
  }

  public SrcClass makeSrcClassStub( String fqn, JavaFileManager.Location location )
  {
    BasicJavacTask javacTask = location != null && JavacPlugin.instance() != null ? JavacPlugin.instance().getJavacTask() : getJavacTask();
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> pair = getClassSymbol( javacTask, location, fqn );
    if( pair == null )
    {
      throw new IllegalStateException( "Failed to find class, '" + fqn + "'" );
    }
    Symbol.ClassSymbol classSymbol = pair.getFirst();
    if( classSymbol == null )
    {
      // For the case where the class is generated from a type manifold esp. from a IExtensionClassProducer
      return makeSrcClassStubFromProducedClass( fqn, location );
    }

    return SrcClassUtil.instance().makeStub( _module, fqn, classSymbol, pair.getSecond(), getJavacTask() );
  }

  private SrcClass makeSrcClassStubFromProducedClass( String fqn, JavaFileManager.Location location )
  {
    BasicJavacTask[] task = new BasicJavacTask[1];
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> pair = getClassSymbolForProducedClass( fqn, task );
    if( pair == null )
    {
      throw new NullPointerException( "Could not find ClassSymbol for: " + fqn );
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
    if( _wfm == null )
    {
      _wfm = new ManifoldJavaFileManager( _fm, null, false );
    }
    task[0] = (BasicJavacTask)_javacTool.getTask( errors, _wfm, null, Arrays.asList( "-proc:none", "-source", "1.8", "-Xprefer:source" ), null, Collections.singleton( fileObj.getFirst() ) );

    // note, ok to call getTypeElement() directly here and not via IDynamicJdk because always in context of 1.8 (no module)
    JavacElements elementUtils = JavacElements.instance( task[0].getContext() );
    Symbol.ClassSymbol e = elementUtils.getTypeElement( fqn );

    if( e != null && e.getSimpleName().contentEquals( ManClassUtil.getShortClassName( fqn ) ) )
    {
      JavacTrees trees = JavacTrees.instance( task[0].getContext() );
      TreePath path = trees.getPath( e );
      if( path != null )
      {
        return new Pair<>( e, (JCTree.JCCompilationUnit)path.getCompilationUnit() );
      }
      else
      {
        // TreePath is only applicable to a source file;
        // if fqn is not a source file, there is no compilation unit available
        return new Pair<>( e, null );
      }
    }

    StringBuffer errorText = errors.getBuffer();
    if( errorText.length() > 0 )
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
