package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import manifold.util.JavacDiagnostic;

public abstract class CompiledTypeProcessor implements TaskListener
{
  private final JavacTask _javacTask;
  private CompilationUnitTree _compilationUnit;
  private final Types _types;
  private final Map<String, Boolean> _typesToProcess;
  private final IssueReporter<JavaFileObject> _issueReporter;
  private Map<String, JCTree.JCClassDecl> _innerClassForGeneration;
  private JCTree.JCClassDecl _tree;
  private boolean _generate;

  CompiledTypeProcessor( JavacTask javacTask )
  {
    _javacTask = javacTask;
    javacTask.addTaskListener( this );
    Context context = ((BasicJavacTask)javacTask).getContext();
    JavaCompiler compiler = JavaCompiler.instance( context );
    compiler.shouldStopPolicyIfNoError = CompileState.max( compiler.shouldStopPolicyIfNoError, CompileState.FLOW );
    _issueReporter = new IssueReporter<>( Log.instance( context ) );
    _types = Types.instance( context );
//    DiagnosticListener dl = context.get( DiagnosticListener.class );
//    context.put( DiagnosticListener.class, (DiagnosticListener)null );
//    context.put( DiagnosticListener.class, new WrappedDiagnosticListener( dl ) );

    _typesToProcess = new HashMap<>();
    _innerClassForGeneration = new HashMap<>();
  }

  /**
   * Subclasses override to process a compiled type.
   */
  public abstract void process( TypeElement element, IssueReporter<JavaFileObject> issueReporter );

//  /**
//   * Subclasses override to filter javac compile errors / warnings.
//   */
//  public abstract boolean filterError( Diagnostic diagnostic );


  public Context getContext()
  {
    return ((BasicJavacTask)getJavacTask()).getContext();
  }

  public JavacTask getJavacTask()
  {
    return _javacTask;
  }

  public JCTree.JCClassDecl getTree()
  {
    return _tree;
  }

  public boolean isGenerate()
  {
    return _generate;
  }

  public CompilationUnitTree getCompilationUnit()
  {
    return _compilationUnit;
  }

  public Types getTypes()
  {
    return _types;
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

  public Symtab getSymtab()
  {
    return Symtab.instance( getContext() );
  }

  public TreePath2 getPath( Tree node )
  {
    return TreePath2.getPath( getCompilationUnit(), node );
  }

  public Tree getParent( Tree node )
  {
    TreePath2 path = TreePath2.getPath( getCompilationUnit(), node );
    if( path == null )
    {
      // null is indiciative of Generation phase where trees are no longer attached to symobls so the comp unit is detached
      // use the root tree instead, which is mostly ok, mostly
      path = TreePath2.getPath( _tree, node );
    }
    TreePath2 parentPath = path.getParentPath();
    return parentPath == null ? null : parentPath.getLeaf();
  }

  public JCTree.JCClassDecl getClassDecl( Tree node )
  {
    if( node == null || node instanceof JCTree.JCCompilationUnit )
    {
      return null;
    }

    if( node instanceof JCTree.JCClassDecl )
    {
      return (JCTree.JCClassDecl)node;
    }

    return getClassDecl( getParent( node ) );
  }

  public JavaFileObject getFile( Tree node )
  {
    JCTree.JCClassDecl classDecl = getClassDecl( node );
    return classDecl == null ? null : classDecl.sym.sourcefile;
  }

  public void report( JCTree tree, Diagnostic.Kind kind, String msg )
  {
    IssueReporter<JavaFileObject> reporter = new IssueReporter<>( Log.instance( getContext() ) );
    JavaFileObject file = getFile( tree );
    reporter.report( new JavacDiagnostic( file, kind, tree.getStartPosition(), 0, 0, msg ) );
  }

  public boolean addTypesToProcess( RoundEnvironment roundEnv )
  {
    for( TypeElement elem : ElementFilter.typesIn( roundEnv.getRootElements() ) )
    {
      _typesToProcess.put( elem.getQualifiedName().toString(), false );
    }
    return false;
  }

  public void addTypesToProcess( Set<String> types )
  {
    types.forEach( e -> _typesToProcess.put( e, false ) );
  }

  @Override
  public void started( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.GENERATE )
    {
      return;
    }

    //
    // Process trees that were generated and therefore not available during ANALYZE
    // For instance, we must process bridge methods
    //

    TypeElement elem = e.getTypeElement();

    if( elem instanceof Symbol.ClassSymbol )
    {
      if( _typesToProcess.containsKey( elem.getQualifiedName().toString() ) )
      {
        _tree = findTopLevel( (Symbol.ClassSymbol)elem, e.getCompilationUnit().getTypeDecls() );
      }
      else
      {
        _tree = _innerClassForGeneration.get( ((Symbol.ClassSymbol)elem).flatName().toString() );
      }

      if( _tree != null )
      {
        _compilationUnit = e.getCompilationUnit();
        _generate = true;
        process( elem, _issueReporter );
      }
    }
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ANALYZE )
    {
      return;
    }

    //
    // Process fully analyzed trees (full type information is in the trees)
    //

    _generate = false;

    String fqn = e.getTypeElement().getQualifiedName().toString();
    Boolean visited = _typesToProcess.get( fqn );
    if( visited == Boolean.TRUE )
    {
      // already processed
      return;
    }
//    if( visited == null && !isNested( e.getTypeElement().getEnclosingElement() ) && !isOuter( fqn ) )
//    {
//      // also process inner types of types to process and (outer type if processing inner type first)
//      return;
//    }

    if( fqn.isEmpty() )
    {
      return;
    }
    
    // mark processed
    _typesToProcess.put( fqn, true );

    _compilationUnit = e.getCompilationUnit();

    TypeElement elem = e.getTypeElement();
    _tree = (JCTree.JCClassDecl)getTreeUtil().getTree( elem );
    preserveInnerClassesForGeneration( _tree );

    process( elem, _issueReporter );
  }

  private JCTree.JCClassDecl findTopLevel( Symbol.ClassSymbol type, List<? extends Tree> typeDecls )
  {
    for( Tree tree: typeDecls )
    {
      if( tree instanceof JCTree.JCClassDecl && ((JCTree.JCClassDecl)tree).sym == type )
      {
        return (JCTree.JCClassDecl)tree;
      }
    }
    return null;
  }

  private void preserveInnerClassesForGeneration( JCTree.JCClassDecl tree )
  {
    for( JCTree def: tree.defs )
    {
      if( def instanceof JCTree.JCClassDecl )
      {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)def;

        preserveInnerClassForGenerationPhase( classDecl );
        preserveInnerClassesForGeneration( classDecl );
      }
    }
  }

  public void preserveInnerClassForGenerationPhase( JCTree.JCClassDecl def )
  {
    _innerClassForGeneration.put( def.sym.flatName().toString(), def );
  }

  private boolean isNested( Element elem )
  {
    if( !(elem instanceof TypeElement) )
    {
      return false;
    }
    TypeElement typeElem = (TypeElement)elem;
    String fqn = typeElem.getQualifiedName().toString();
    if( _typesToProcess.containsKey( fqn ) )
    {
      return true;
    }
    for( String t: _typesToProcess.keySet() )
    {
      if( t.contains( fqn + '.' ) )
      {
        return true;
      }
    }
    return isNested( typeElem.getEnclosingElement() );
  }
  private boolean isOuter( String fqn )
  {
    if( fqn.isEmpty() )
    {
      return false;
    }
    for( String t: _typesToProcess.keySet() )
    {
      if( t.contains( fqn + '.' ) )
      {
        return true;
      }
    }
    return false;
  }

  private class AnonymousClassListener implements Scope.ScopeListener
  {
    JCTree.JCClassDecl _tree;

    public AnonymousClassListener( JCTree.JCClassDecl tree )
    {
      _tree = tree;
    }

    @Override
    public void symbolAdded( Symbol sym, Scope s )
    {
      if( sym instanceof Symbol.ClassSymbol && sym.isAnonymous() )
      {
        System.out.println( sym.getQualifiedName() );
      }
    }

    @Override
    public void symbolRemoved( Symbol sym, Scope s )
    {

    }
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