/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import java.util.HashMap;
import java.util.Map;
import manifold.api.util.Pair;


class JavacBinder extends AbstractBinder<Symbol.MethodSymbol, JCBinary, JCExpression, Tag>
{
  private final Map<Pair<Type, Type>, Symbol.MethodSymbol> _mapReactions;
  private final Types _types;
  private final TreeMaker _make;

  JavacBinder( Types types )
  {
    _mapReactions = new HashMap<>();
    _types = types;
    _make = JavacPlugin.instance().getTreeMaker();
  }

  @Override
  protected Symbol.MethodSymbol findBinderMethod( Node<JCExpression, Tag> left, Node<JCExpression, Tag> right )
  {
    Type lhs = left._expr.type;
    Type rhs = right._expr.type;
    Pair<Type, Type> pair = Pair.make( lhs, rhs );
    if( right._operatorLeft == null && _mapReactions.containsKey( pair ) )
    {
      return _mapReactions.get( pair );
    }
    Symbol.MethodSymbol reaction = getReaction( lhs, rhs, right._operatorLeft );
    if( right._operatorLeft == null )
    {
      _mapReactions.put( pair, reaction );
    }
    return reaction;
  }

  private Symbol.MethodSymbol getReaction( Type lhs, Type rhs, Tag operator )
  {
    if( operator != null )
    {
      return resolveOperatorMethod( lhs, rhs, operator );
    }
    else
    {
      Symbol.MethodSymbol binder = resolveBinderMethod( "prefixBind", lhs, rhs );
      if( binder == null )
      {
        binder = resolveBinderMethod( "postfixBind", rhs, lhs );
      }
      return binder;
    }
  }

  private Symbol.MethodSymbol resolveOperatorMethod( Type left, Type right, Tag operator )
  {
    // Handle operator overloading

    boolean swapped = false;
    Symbol.MethodSymbol overloadOperator = ManAttr.resolveOperatorMethod( _types, operator, left, right );
    if( overloadOperator == null && ManAttr.isCommutative( operator ) )
    {
      overloadOperator = ManAttr.resolveOperatorMethod( _types, operator, right, left );
      swapped = true;
    }
    if( overloadOperator != null )
    {
      return new OverloadOperatorSymbol( overloadOperator, swapped );
    }

    return null;
  }

  private Symbol.MethodSymbol resolveBinderMethod( String name, Type left, Type right )
  {
    if( !(left.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    return ManAttr.getMethodSymbol( _types, left, right, name, (Symbol.ClassSymbol)left.tsym, 1 );
  }

  @Override
  protected Node<JCExpression, Tag> makeBinaryExpression( Node<JCExpression, Tag> left,
                                                                 Node<JCExpression, Tag> right,
                                                                 Symbol.MethodSymbol binderMethod )
  {
    JCBinary binary = _make.Binary( right._operatorLeft == null
                                    ? Tag.MUL
                                    : right._operatorLeft, left._expr, right._expr );
    binary.pos = left._expr.pos;
    boolean rightToLeft =
      binderMethod instanceof OverloadOperatorSymbol && ((OverloadOperatorSymbol)binderMethod).isSwapped() ||
      binderMethod.name.toString().equals( "postfixBind" );
    IDynamicJdk.instance().setOperator( binary, new OverloadOperatorSymbol( binderMethod, rightToLeft ) );
    binary.type = rightToLeft
                  ? _types.memberType( right._expr.type, binderMethod ).getReturnType()
                  : _types.memberType( left._expr.type, binderMethod ).getReturnType();
    return new Node<>( binary, left._operatorLeft );
  }
}
