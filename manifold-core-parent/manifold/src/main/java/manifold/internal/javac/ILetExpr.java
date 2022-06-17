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

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

/**
 * The LetExpr has evolved a little over time regarding the declared type of {@link LetExpr#defs} and
 * {@link LetExpr#expr}. Manifold uses LetExpr to handle JCUnary inc/dec and JCAssignOp (+=, *=, etc.) with advanced
 * features such as operator overloading and properties. In some cases, such as properties, we need the LetExpr to
 * have defs that are not JCVarDecl e.g., setXxx() to handle JCAssignOp. All of this mumbo jumbo here and in LetExpr_8
 * and LetExpr_11 is to make that work.
 */
public interface ILetExpr
{
  /**
   * Always use this method to make a new LetExpr, which must be an instance of either LetExpr_8 or LetExpr_11.
   */
  static LetExpr makeLetExpr(
    TreeMaker make, List<? extends JCTree> tempVars, JCTree.JCExpression value, Type type, int pos )
  {
    // Need let expr so that we can return the RHS value as required by java assignment op.
    // Note, the setXxx() method can return whatever it wants, it is ignored here,
    // this allows us to support eg. List.set(int, T) where this method returns the previous value
    LetExpr letExpr = (LetExpr)ReflectUtil.method( make, "LetExpr",
      List.class, JreUtil.isJava8() ? JCTree.class : JCTree.JCExpression.class )
      .invoke( tempVars, value );
    if( type != null )
    {
      // if the type is a constant expr, the Generator will optimize out the LetExpr,
      // so we have to put in the non-constant type (wtaf)
      letExpr.type = type.constValue() != null ? type.baseType() : type;
    }
    letExpr.setPos( pos );
    return (LetExpr)(JreUtil.isJava8()
          ? (JCTree)ReflectUtil.constructor( "manifold.internal.javac.LetExpr_8", LetExpr.class ).newInstance( letExpr )
          : (JCTree)ReflectUtil.constructor( "manifold.internal.javac.LetExpr_11", LetExpr.class ).newInstance( letExpr ));
  }

  List<JCTree.JCStatement> getDefs();
  JCTree.JCExpression getExpr();
}
