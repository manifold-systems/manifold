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

package manifold.preprocessor.expression;

import com.sun.corba.se.spi.ior.IdentifiableFactory;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.definitions.Definitions;

import java.math.BigDecimal;
import java.util.*;

public class RelationalExpression extends Expression
{
  private final Expression _lhs;
  private final Expression _rhs;
  private final Operator _op;

  enum Operator
  {
    GT(">"), GE(">="), LT("<"), LE("<=");

    private final String _op;

    Operator( String op )
    {
      _op = op;
    }

    public String toString()
    {
      return _op;
    }

    static Operator from( String op )
    {
      switch( op )
      {
        case ">": return GT;
        case ">=": return GE;
        case "<": return LT;
        case "<=": return LE;
      }
      throw new IllegalArgumentException();
    }
  }

  RelationalExpression( Expression lhs, Expression rhs, String op, int start, int end )
  {
    super( start, end );
    _lhs = lhs;
    _rhs = rhs;
    _op = Operator.from( op );
  }

  @Override
  public List<Expression> getChildren()
  {
    ArrayList<Expression> children = new ArrayList<>();
    children.add( _lhs );
    children.add( _rhs );
    return children;
  }

  /**
   * True iff string values of operands are coercible to {@link BigDecimal} and result of
   * {@code LHS.compareTo(RHS) [op] 0} returns true
   */
  @Override
  public boolean evaluate( Definitions definitions )
  {
    BigDecimal lhsNumber = getNumberValue( _lhs, definitions );
    BigDecimal rhsNumber = getNumberValue( _rhs, definitions );
    if( lhsNumber == null || rhsNumber == null )
    {
      return false;
    }
    switch( _op )
    {
      case GT: return lhsNumber.compareTo( rhsNumber ) > 0;
      case GE: return lhsNumber.compareTo( rhsNumber ) >= 0;
      case LT: return lhsNumber.compareTo( rhsNumber ) < 0;
      case LE: return lhsNumber.compareTo( rhsNumber ) <= 0;
    }
    throw new IllegalStateException();
  }

  public BigDecimal getNumberValue( Expression operand, Definitions definitions )
  {
    String value = operand.getValue( definitions );
    try
    {
      return new BigDecimal( value.trim() );
    }
    catch( Exception e )
    {
      generateError( "\"" + value + "\"" );
    }
    return null;
  }

  private void generateError( String value )
  {
    if( JavacPlugin.instance() != null )
    {
      IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ),
        new JCDiagnostic.SimpleDiagnosticPosition( getStartOffset() ),
        "proc.messager", "Non-numeric value not allowed in relational expression: " + value );
    }
  }

  public String toString()
  {
    return _lhs + " " + _op + " " + _rhs;
  }
}
