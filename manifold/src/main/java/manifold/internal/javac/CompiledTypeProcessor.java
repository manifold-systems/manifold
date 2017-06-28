package manifold.internal.javac;

import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;

public abstract class CompiledTypeProcessor implements TaskListener
{
  private final JavacTask _javacTask;
  private JCTree.JCCompilationUnit _compilationUnit;
  private final Set<String> _typesToProcess;
  private final IssueReporter<JavaFileObject> _issueReporter;

  CompiledTypeProcessor( JavacTask javacTask )
  {
    _javacTask = javacTask;
    javacTask.addTaskListener( this );
    Context context = ((JavacTaskImpl)javacTask).getContext();
    JavaCompiler compiler = JavaCompiler.instance( context );
    compiler.shouldStopPolicyIfNoError = CompileState.max( compiler.shouldStopPolicyIfNoError, CompileState.FLOW );
    _issueReporter = new IssueReporter<>( Log.instance( context ) );

//    DiagnosticListener dl = context.get( DiagnosticListener.class );
//    context.put( DiagnosticListener.class, (DiagnosticListener)null );
//    context.put( DiagnosticListener.class, new WrappedDiagnosticListener( dl ) );

    _typesToProcess = new HashSet<>();
  }

  /**
   * Subclasses override to process a compiled type.
   */
  public abstract void process( TypeElement element, TreePath tree, IssueReporter<JavaFileObject> issueReporter );

//  /**
//   * Subclasses override to filter javac compile errors / warnings.
//   */
//  public abstract boolean filterError( Diagnostic diagnostic );


  public Context getContext()
  {
    return ((JavacTaskImpl)getJavacTask()).getContext();
  }

  public JavacTask getJavacTask()
  {
    return _javacTask;
  }

  public JCTree.JCCompilationUnit getCompilationUnit()
  {
    return _compilationUnit;
  }

  public JavacElements getElementUtil()
  {
    return JavacElements.instance( getContext() );
  }

  public Trees getTreeUtil()
  {
    return Trees.instance( getJavacTask() );
  }

  public TreeMaker getTreeMaker()
  {
    return TreeMaker.instance( getContext() );
  }

  public TreePath getPath( Tree node )
  {
    return getTreeUtil().getPath( getCompilationUnit(), node );
  }

  public Tree getParent( Tree node )
  {
    TreePath parentPath = getTreeUtil().getPath( getCompilationUnit(), node ).getParentPath();
    return parentPath == null ? null : parentPath.getLeaf();
  }
  
  public boolean addTypesToProcess( RoundEnvironment roundEnv )
  {
    for( TypeElement elem : ElementFilter.typesIn( roundEnv.getRootElements() ) )
    {
      _typesToProcess.add( elem.getQualifiedName().toString() );
    }
    return false;
  }

  public void addTypesToProcess( Set<String> types )
  {
    _typesToProcess.addAll( types );
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ANALYZE )
    {
      return;
    }

    if( !_typesToProcess.remove( e.getTypeElement().getQualifiedName().toString() ) )
    {
      return;
    }

    _compilationUnit = (JCTree.JCCompilationUnit)e.getCompilationUnit();

    TypeElement elem = e.getTypeElement();
    TreePath path = Trees.instance( _javacTask ).getPath( elem );

    process( elem, path, _issueReporter );
  }

  @Override
  public void started( TaskEvent e )
  {
  }

//  private class WrappedDiagnosticListener implements DiagnosticListener
//  {
//    private final DiagnosticListener _dl;
//
//    public WrappedDiagnosticListener( DiagnosticListener dl )
//    {
//      _dl = dl;
//    }
//
//    @Override
//    public void report( Diagnostic diagnostic )
//    {
//      if( !filterError( diagnostic ) )
//      {
//        _dl.report( diagnostic );
//      }
//    }
//  }
}