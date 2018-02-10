package manifold.internal.javac;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import manifold.api.host.NoBootstrap;
import manifold.internal.runtime.Bootstrap;


/**
 * Add a static block to top-level classes to demo Manifold:
 * <pre>
 *   static {
 *     Bootstrap.init();
 *   }
 * </pre>
 * Note this call is fast and does nothing if Manifold is already bootstrapped.
 */
class BootstrapInserter extends TreeTranslator
{
  private JavacPlugin _javacJacker;

  public BootstrapInserter( JavacPlugin javacJacker )
  {
    _javacJacker = javacJacker;
  }

  @Override
  public void visitClassDef( JCTree.JCClassDecl tree )
  {
    super.visitClassDef( tree );
    if( tree.sym != null && !tree.sym.isInner() )
    {
      if( okToInsertBootstrap( tree ) )
      {
        JCTree.JCStatement newNode = buildBootstrapStaticBlock();
        ArrayList<JCTree> newDefs = new ArrayList<>( tree.defs );
        newDefs.add( 0, newNode );
        tree.defs = List.from( newDefs );
      }
    }
    result = tree;
  }

  private boolean okToInsertBootstrap( JCTree.JCClassDecl tree )
  {
    return !annotatedWith_NoBootstrap( tree.getModifiers().getAnnotations() ) &&
           !JavacPlugin.instance().isNoBootstrapping() &&
           !skipForOtherReasons( tree );
  }

  private boolean skipForOtherReasons( JCTree.JCClassDecl tree )
  {
    if( (tree.getModifiers().flags & Flags.ANNOTATION) != 0 )
    {
      // don't bootstrap from an annotation class,
      // many tools do not handle the presence of the <clinit> method well
      return true;
    }
    return false;
  }

  private boolean annotatedWith_NoBootstrap( List<JCTree.JCAnnotation> annotations )
  {
    for( JCTree.JCAnnotation anno : annotations )
    {
      if( anno.getAnnotationType().toString().endsWith( NoBootstrap.class.getSimpleName() ) )
      {
        return true;
      }
    }
    return false;
  }

  private JCTree.JCStatement buildBootstrapStaticBlock()
  {
    TreeMaker make = _javacJacker.getTreeMaker();
    JavacElements javacElems = _javacJacker.getJavacElements();

    JCTree.JCMethodInvocation bootstrapInitCall = make.Apply( List.nil(), memberAccess( make, javacElems, Bootstrap.class.getName() + ".init" ), List.nil() );
    return make.Block( Modifier.STATIC, List.of( make.Exec( bootstrapInitCall ) ) );
  }

  private JCTree.JCExpression memberAccess( TreeMaker make, JavacElements javacElems, String path )
  {
    return memberAccess( make, javacElems, path.split( "\\." ) );
  }

  private JCTree.JCExpression memberAccess( TreeMaker make, JavacElements node, String... components )
  {
    JCTree.JCExpression expr = make.Ident( node.getName( components[0] ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, node.getName( components[i] ) );
    }
    return expr;
  }
}
