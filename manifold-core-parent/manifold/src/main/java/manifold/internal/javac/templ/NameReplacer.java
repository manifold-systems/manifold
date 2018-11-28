package manifold.internal.javac.templ;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Names;

class NameReplacer extends TreeTranslator
{
  private final BasicJavacTask _javacTask;
  private final int _offset;

  NameReplacer( BasicJavacTask javacTask, int offset )
  {
    _offset = offset;
    _javacTask = javacTask;
  }

  @Override
  public void visitIdent( JCTree.JCIdent jcIdent )
  {
    super.visitIdent( jcIdent );
    Names names = Names.instance( _javacTask.getContext() );
    jcIdent.name = names.fromString( jcIdent.name.toString() );
    jcIdent.pos = _offset;
  }

  @Override
  public void visitSelect( JCTree.JCFieldAccess jcFieldAccess )
  {
    super.visitSelect( jcFieldAccess );
    Names names = Names.instance( _javacTask.getContext() );
    jcFieldAccess.name = names.fromString( jcFieldAccess.name.toString() );
    jcFieldAccess.pos = _offset;
  }
}
