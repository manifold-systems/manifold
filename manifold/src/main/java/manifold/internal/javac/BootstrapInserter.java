package manifold.internal.javac;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import manifold.internal.runtime.Bootstrap;


/**
 * Add a static block to top-level classes to bootstrap Gosu:
 * <pre>
 *   static {
 *     Bootstrap.init();
 *   }
 * </pre>
 * Note this call is fast and does nothing if Gosu is already bootstrapped.
 */
class BootstrapInserter extends TreeTranslator
{
  private JavacHook _javacJacker;

  public BootstrapInserter( JavacHook javacJacker )
  {
    _javacJacker = javacJacker;
  }

  @Override
  public void visitClassDef( JCTree.JCClassDecl tree )
  {
    super.visitClassDef( tree );
    if( tree.sym != null && !tree.sym.isInner() )
    {
      JCTree.JCStatement newNode = buildBootstrapStaticBlock();
      ArrayList<JCTree> newDefs = new ArrayList<>( tree.defs );
      newDefs.add( 0, newNode );
      tree.defs = List.from( newDefs );
    }
    result = tree;
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
