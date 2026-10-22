package manifold.ext.parts;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;

import java.util.HashSet;
import java.util.Set;

/**
 * Clears sym/type on idents that refer to the reference method's type variables,
 * so MemberEnter/Attr re-resolve them by name against the new method.
 */
class TypeVarScrubber extends TreeScanner
{
  private final Set<Symbol> oldTvarSyms = new HashSet<>();

  TypeVarScrubber( List<Type> oldTvars )
  {
    for( Type tv : oldTvars )
    {
      oldTvarSyms.add( tv.tsym );
    }
  }

  <T extends JCTree> T scrub( T tree )
  {
    if( tree != null )
    {
      scan( tree );
    }
    return tree;
  }

  <T extends JCTree> List<T> scrub( List<T> trees )
  {
    for( T t : trees )
    {
      scrub( t );
    }
    return trees;
  }

  @Override
  public void visitIdent( JCTree.JCIdent id )
  {
    if( oldTvarSyms.contains( id.sym ) )
    {
      id.sym = null;
      id.type = null;
    }
  }
}
