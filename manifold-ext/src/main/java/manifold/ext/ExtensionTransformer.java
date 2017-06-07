package manifold.ext;

import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import java.util.ArrayList;
import manifold.ext.api.ExtensionMethod;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.JavacHook;
import manifold.internal.javac.TypeProcessor;

/**
 */
class ExtensionTransformer extends TreeTranslator
{
  private final ExtSourceProducer _sp;
  private final TypeProcessor _tp;

  ExtensionTransformer( ExtSourceProducer sp, TypeProcessor typeProcessor )
  {
    _sp = sp;
    _tp = typeProcessor;
  }

  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    super.visitApply( tree );
    Symbol.MethodSymbol method = findExtMethod( tree );
    if( method != null )
    {
      replaceCall( tree, method );
    }
    result = tree;
  }

  private void replaceCall( JCTree.JCMethodInvocation tree, Symbol.MethodSymbol method )
  {
    JCTree.JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCTree.JCExpression thisArg = m.selected;
      String extensionFqn = method.getEnclosingElement().asType().tsym.toString();
      m.selected = memberAccess( make, javacElems, extensionFqn );
      JavacTaskImpl javacTask = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getJavacTask();
      Symbol.ClassSymbol extensionClassSym = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getClassSymbol( javacTask, extensionFqn );
      assignTypes( m.selected, extensionClassSym );
      m.sym = method;
      m.type = method.type;

      ArrayList<JCTree.JCExpression> newArgs = new ArrayList<>( tree.args );
      newArgs.add( 0, thisArg );

      tree.args = List.from( newArgs );
    }
  }

  private void assignTypes( JCTree.JCExpression m, Symbol symbol )
  {
    if( m instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)m;
      fieldAccess.sym = symbol;
      fieldAccess.type = symbol.type;
      assignTypes( fieldAccess.selected, symbol.owner );
    }
    else if( m instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent fieldAccess = (JCTree.JCIdent)m;
      fieldAccess.sym = symbol;
      fieldAccess.type = symbol.type;
    }
  }

  private Symbol.MethodSymbol findExtMethod( JCTree.JCMethodInvocation tree )
  {
    JCTree.JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof MemberSelectTree )
    {
      JCTree.JCFieldAccess meth = (JCTree.JCFieldAccess)tree.meth;
      if( !meth.sym.hasAnnotations() )
      {
        return null;
      }
      for( Attribute.Compound annotation : meth.sym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
        {
          String extensionClass = (String)annotation.values.get( 0 ).snd.getValue();
          JavacTaskImpl javacTask = (JavacTaskImpl)JavacHook.instance().getJavacTask();
          Symbol.ClassSymbol extClassSym = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getClassSymbol( javacTask, extensionClass );
          Types types = Types.instance( javacTask.getContext() );
          outer:
          for( Symbol elem : extClassSym.members().getElements() )
          {
            if( elem instanceof Symbol.MethodSymbol && elem.flatName().toString().equals( meth.sym.name.toString() ) )
            {
              Symbol.MethodSymbol extMethodSym = (Symbol.MethodSymbol)elem;
              List<Symbol.VarSymbol> extParams = extMethodSym.getParameters();
              List<Symbol.VarSymbol> calledParams = ((Symbol.MethodSymbol)meth.sym).getParameters();
              if( extParams.size() - 1 != calledParams.size() )
              {
                continue;
              }
              for( int i = 1; i < extParams.size(); i++ )
              {
                Symbol.VarSymbol extParam = extParams.get( i );
                Symbol.VarSymbol calledParam = calledParams.get( i - 1 );
                if( !types.isSameType( types.erasure( extParam.type ), types.erasure( calledParam.type ) ) )
                {
                  continue outer;
                }
              }
              return extMethodSym;
            }
          }
        }
      }
    }
    return null;
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
