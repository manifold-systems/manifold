/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;

public class ParseProcessor extends TreeTranslator
{
  private final JavacPlugin _javacPlugin;

  public ParseProcessor( JavacPlugin javacPlugin )
  {
    _javacPlugin = javacPlugin;
  }

  @Override
  public void visitAssignop( JCTree.JCAssignOp tree )
  {
    super.visitAssignop( tree );

    if( !JavacPlugin.instance().isExtensionsEnabled() )
    {
      // operator overloading requires manifold-ext
      return;
    }

    // transform a += b  to  a = a + b, so that operator overloading can use plus() to implement this

    TreeMaker make = _javacPlugin.getTreeMaker();
    JCTree.Tag binop = tree.getTag().noAssignOp();

    JCTree.JCBinary binary = make.Binary( binop, tree.lhs, tree.rhs );
    binary.pos = tree.rhs.pos;
    JCTree.JCAssign assign =
      make.Assign( (JCTree.JCExpression)tree.lhs.clone(), binary );
    assign.pos = tree.pos;
    result = assign;
  }
}
