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

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * expression (velocity: length/time): 5 mi/hr
 * 
 *   (bad)   (good)
 *     ÷        ?
 *    / \      / \
 *   ?   hr   5   ÷
 *  / \          / \
 * 5   mi       mi  hr
 *
 * </pre>
 * <p>
 * A list of all the terminal operands maintains the operators in RHS operand nodes. The binding algorithm uses type
 * information to test for operator compatibility and binding reactions between adjacent operands to recursively reduce
 * the list to a single expression:
 * <pre>
 * initial list ==>  (? 5) -> (? mi) -> (/ hr)
 * ==>  (5) -> (? (mi/hr))
 * ==>  ((5(mi/hr)))
 * </pre>
 */
public abstract class AbstractBinder<MS, B extends E, E, O>
{
  abstract protected MS findBinderMethod( Node<E, O> left, Node<E, O> right );

  abstract protected Node<E, O> makeBinaryExpression( Node<E, O> left, Node<E, O> right, MS binderMethod );

  abstract protected B leftAssociateMuls( B solution );

  @SuppressWarnings("WeakerAccess")
  public B bind( ArrayList<Node<E, O>> operands )
  {
    if( operands.isEmpty() )
    {
      return null;
    }

    if( operands.size() == 1 )
    {
      //noinspection unchecked
      return (B)operands.get( 0 ).getExpr();
    }

    for( Root root = nextRoot( operands, 0 ); root != null;
         root = nextRoot( operands, root._index + 1 ) )
    {
      //noinspection unchecked
      ArrayList<Node<E, O>> reduced = (ArrayList<Node<E, O>>)operands.clone();
      root.replaceWithPair( reduced );
      B solution = bind( reduced );
      if( solution != null )
      {
        return leftAssociateMuls( solution );
      }
    }
    return null;
  }

  private Root nextRoot( List<Node<E, O>> operands, int startIndex )
  {
    Node<E, O> left = null;
    for( int i = startIndex; i < operands.size(); i++ )
    {
      Node<E, O> right = operands.get( i );
      if( left != null )
      {
        MS binderMethod = findBinderMethod( left, right );
        if( binderMethod != null )
        {
          return new Root( i - 1, binderMethod );
        }
      }
      left = right;
    }
    return null;
  }

  private class Root
  {
    int _index;
    MS _binderMethod;

    Root( int index, MS binderMethod )
    {
      _index = index;
      _binderMethod = binderMethod;
    }

    private void replaceWithPair( List<Node<E, O>> operands )
    {
      Node<E, O> left = operands.get( _index );
      operands.remove( _index );
      Node<E, O> right = operands.get( _index );
      Node<E, O> rootExpr = makeBinaryExpression( left, right, _binderMethod );
      operands.set( _index, rootExpr );
    }
  }

  public static class Node<E, O>
  {
    E _expr;
    O _operatorLeft;

    public Node( E expr )
    {
      this( expr, null );
    }

    public Node( E expr, O operatorLeft )
    {
      _expr = expr;
      _operatorLeft = operatorLeft;
    }

    public E getExpr()
    {
      return _expr;
    }

    @SuppressWarnings("unused")
    public O getOperatorLeft()
    {
      return _operatorLeft;
    }
    @SuppressWarnings("unused")
    public void setOperatorLeft( O value )
    {
      _operatorLeft = value;
    }
  }
}
