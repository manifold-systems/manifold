package manifold.internal.javac;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Names;

class NameReplacer extends TreeTranslator
{
  private final Names _names;
  private final int _offset;

  NameReplacer( BasicJavacTask javacTask, int offset )
  {
    _names = Names.instance( javacTask.getContext() );
    _offset = offset;
  }

  @Override
  public void visitIdent( JCTree.JCIdent jcIdent )
  {
    super.visitIdent( jcIdent );
    jcIdent.name = _names.fromString( jcIdent.name.toString() );
    jcIdent.pos = _offset;
  }

  @Override
  public void visitSelect( JCTree.JCFieldAccess jcFieldAccess )
  {
    super.visitSelect( jcFieldAccess );
    jcFieldAccess.name = _names.fromString( jcFieldAccess.name.toString() );
    jcFieldAccess.pos = _offset;
  }
}
