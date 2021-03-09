/*
 * Copyright (c) 2021 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.internal.javac;

import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import manifold.util.ReflectUtil;

public class LetExpr_8 extends JCTree.LetExpr implements ILetExpr
{
  protected LetExpr_8( LetExpr tree )
  {
    super( tree.defs, tree.expr );
    pos = tree.pos;
    type = tree.type;
  }

  @Override
  public List<JCStatement> getDefs()
  {
    //noinspection unchecked
    return (List)defs;
  }

  @Override
  public JCExpression getExpr()
  {
    return (JCExpression)expr;
  }

  public Kind getKind()
  {
    return null;
  }

  @Override
  public void accept( Visitor v )
  {
    if( v instanceof TreeTranslator )
    {
      // Total unforgivable hack to make LetExpr have non-JCVarDecls in the defs (e.g., method calls so properties can
      // handle AssignOp). This is important so LetExprs can be used more broadly to handle otherwise impossible to
      // implement features, such as JCUnary inc/dec and JCAssignOp (+=, *=, etc.) with operator overloading and
      // properties. Note LetExpr are used way up in the Parse phase to transform JCAssignOp expressions to regular
      // JCAssign expressions, which simplifies operator overloading and properties implementations.
      //
      // Also note, Java 16 changes LetExpr once again to allow arbitrary statements by changing the type of defs to
      // List<JCStatement>, which is exactly what we want. It still doesn't handle LetExpr at the lower compiler stages,
      // like Parse, however there is some satisfaction in seeing them move in the same direction.
      //
      visitLetExprForTreeTranslator( (TreeTranslator)v, this );
    }
    else
    {
      super.accept( v );
    }
  }
  private void visitLetExprForTreeTranslator( TreeTranslator tt, LetExpr tree )
  {
    tree.defs = translateVarDefs( tt, tree.defs );
    tree.expr = tt instanceof Lower ? ((Lower)tt).translate( tree.expr, tree.type ) : tt.translate( tree.expr );
    ReflectUtil.field( tt, "result" ).set( tree );
  }
  private List translateVarDefs( TreeTranslator tt, List<? extends JCTree> trees )
  {
    for( List<JCTree> l = (List)trees; l.nonEmpty(); l = l.tail )
    {
      l.head = tt.translate( l.head );
    }
    return trees;
  }

  @Override
  public <R, D> R accept( TreeVisitor<R, D> v, D d )
  {
    if( v instanceof ParentTreePathScanner )
    {
      //to make LetExpr work with TypeProcessor#getParent()
      return v.visitOther( this, d );
    }
    else
    {
      for( JCStatement def : defs )
      {
        def.accept( v, d );
      }
      return expr.accept( v, d );
    }
  }
}
