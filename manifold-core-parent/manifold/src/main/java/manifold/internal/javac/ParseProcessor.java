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
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.code.Flags.*;

public class ParseProcessor extends TreeTranslator
{
  private final JavacPlugin _javacPlugin;

  public ParseProcessor( JavacPlugin javacPlugin )
  {
    _javacPlugin = javacPlugin;
  }

  private int tempVarIndex = 0;

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

    JCExpression lhs = tree.lhs;
    if( lhs instanceof JCFieldAccess )
    {
      // expr.b += c
      // ---
      // var temp = expr;
      // temp.b = temp.b + c

      JCExpression fieldAccessLhs = ((JCFieldAccess)lhs).selected;
      List<JCTree> tempVars = List.nil();
      tempVarIndex++;
      Context context = JavacPlugin.instance().getContext();
      JCTree[] fieldAccessLhsTemp = tempify( false, fieldAccessLhs, make, fieldAccessLhs, context, "assignTransformLhsTemp" + tempVarIndex, tempVarIndex );
      if( fieldAccessLhsTemp != null )
      {
        tempVars = tempVars.append( fieldAccessLhsTemp[0] );
        fieldAccessLhs = (JCExpression)fieldAccessLhsTemp[1];
      }

      JCFieldAccess newFieldAccess = make.Select( fieldAccessLhs, ((JCFieldAccess)lhs).name );
      newFieldAccess.pos = tree.lhs.pos;
      JCFieldAccess newFieldAccess2 = make.Select( (JCExpression)fieldAccessLhs.clone(), ((JCFieldAccess)lhs).name );
      newFieldAccess2.pos = tree.rhs.pos;
      JCBinary binary = make.Binary( binop, newFieldAccess2, tree.rhs );
      binary.pos = tree.rhs.pos;
      JCAssign assign = make.Assign( newFieldAccess, binary );
      assign.pos = tree.pos;

      if( !tempVars.isEmpty() )
      {
        result = ILetExpr.makeLetExpr( make, tempVars, assign, null, tree.pos );
      }
      else
      {
        result = assign;
      }
    }
    else if( lhs instanceof JCArrayAccess )
    {
      // indexed[index] += c
      // ---
      // var temp1 = indexed;
      // var temp2 = index;
      // temp1[temp2] = temp1[temp2] + c

      Context context = JavacPlugin.instance().getContext();

      JCExpression indexed = ((JCArrayAccess)lhs).indexed;
      List<JCTree> tempVars = List.nil();
      tempVarIndex++;
      JCTree[] indexedTemp = tempify( false, indexed, make, indexed, context, "assignTransformLhsTemp" + tempVarIndex, tempVarIndex );
      if( indexedTemp != null )
      {
        tempVars = tempVars.append( indexedTemp[0] );
        indexed = (JCExpression)indexedTemp[1];
      }

      JCExpression index = ((JCArrayAccess)lhs).index;
      tempVarIndex++;
      JCTree[] indexTemp = tempify( false, index, make, index, context, "assignTransformLhsTemp" + tempVarIndex, tempVarIndex );
      if( indexTemp != null )
      {
        tempVars = tempVars.append( indexTemp[0] );
        index = (JCExpression)indexTemp[1];
      }

      JCArrayAccess newArrayAccess = make.Indexed( indexed, index );
      newArrayAccess.pos = tree.lhs.pos;
      JCArrayAccess newArrayAccess2 = make.Indexed( (JCExpression)indexed.clone(), (JCExpression)index.clone() );
      newArrayAccess.pos = tree.rhs.pos;
      JCBinary binary = make.Binary( binop, newArrayAccess2, tree.rhs );
      binary.pos = tree.rhs.pos;
      JCAssign assign = make.Assign( newArrayAccess, binary );
      assign.pos = tree.pos;

      if( !tempVars.isEmpty() )
      {
        result = ILetExpr.makeLetExpr( make, tempVars, assign, null, tree.pos );
      }
      else
      {
        result = assign;
      }
    }
    else //if( lhs instanceof JCIdent )
    {
      JCBinary binary = make.Binary( binop, tree.lhs, tree.rhs );
      binary.pos = tree.rhs.pos;
      JCAssign assign = make.Assign( (JCExpression)tree.lhs.clone(), binary );
      assign.pos = tree.pos;
      result = assign;
    }
//    }
//    else
//    {
//      throw new UnsupportedOperationException( "Unexpected expression type: " + lhs.getClass().getTypeName() );
//    }
  }

  public static JCTree[] tempify( boolean force, JCTree.JCExpression tree, TreeMaker make, JCExpression expr, Context ctx, String varName, int tempVarIndex )
  {
    switch( expr.getTag() )
    {
      case LITERAL:
      case IDENT:
        if( !force )
        {
          return null;
        }
        // fall through...

      default:
        Names names = Names.instance( JavacPlugin.instance().getContext() );
//        Symtab symtab = Symtab.instance( JavacPlugin.instance().getContext() );
        JCTree.JCVariableDecl tempVar = make.VarDef( make.Modifiers( FINAL /*| SYNTHETIC*/ ),
          Names.instance( ctx ).fromString( varName + tempVarIndex ), make.Ident( names.fromString( Object.class.getSimpleName() ) ), expr );
        tempVar.pos = tree.pos;
        JCExpression ident = make.Ident( tempVar.name );
        ident.pos = tree.pos;
        return new JCTree[]{tempVar, ident};
    }
  }

  private static JCTree.JCExpression memberAccess( TreeMaker make, String path )
  {
    return memberAccess( make, path.split( "\\." ) );
  }

  private static JCTree.JCExpression memberAccess( TreeMaker make, String... components )
  {
    Names names = Names.instance( JavacPlugin.instance().getContext() );
    JCTree.JCExpression expr = make.Ident( names.fromString( ( components[0] ) ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, names.fromString( components[i] ) );
    }
    return expr;
  }

}
