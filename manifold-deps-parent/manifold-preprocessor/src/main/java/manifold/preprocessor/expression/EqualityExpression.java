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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.definitions.Definitions;

import static manifold.preprocessor.expression.ExpressionTokenType.Equals;
import static manifold.preprocessor.expression.ExpressionTokenType.NotEquals;

public class EqualityExpression extends Expression
{
  private final Expression _lhs;
  private final Expression _rhs;
  private final boolean _not;

  EqualityExpression( Expression lhs, Expression rhs, boolean not, int start, int end )
  {
    super( start, end );
    _lhs = lhs;
    _rhs = rhs;
    _not = not;
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
   * True if operands evaluate to the same true/false value AND they have the same value
   */
  @Override
  public boolean evaluate( Definitions definitions )
  {
    if( _lhs instanceof NumberLiteral || _rhs instanceof NumberLiteral )
    {
      // use BD comparison when testing equality with numeric operands

      BigDecimal lhsNumber = getNumberValue( _lhs, definitions );
      if( lhsNumber == null )
      {
        generateError( _lhs, definitions );
        return false;
      }
      BigDecimal rhsNumber = getNumberValue( _rhs, definitions );
      if( rhsNumber == null )
      {
        generateError( _rhs, definitions );
        return false;
      }
      return _not != (lhsNumber.compareTo( rhsNumber ) == 0);
    }
    return _not != (_lhs.evaluate( definitions ) == _rhs.evaluate( definitions ) &&
                    Objects.equals( _lhs.getValue( definitions ), _rhs.getValue( definitions ) ));
  }

  public BigDecimal getNumberValue( Expression operand, Definitions definitions )
  {
    String value = operand.getValue( definitions );
    try
    {
      return new BigDecimal( value.trim() );
    }
    catch( NumberFormatException ignore )
    {
    }
    return null;
  }

  private void generateError( Expression expr, Definitions definitions )
  {
    if( JavacPlugin.instance() != null )
    {
      String value = expr.getValue( definitions );
      IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ),
        new JCDiagnostic.SimpleDiagnosticPosition( expr.getStartOffset() ),
        "proc.messager", "Numeric value expected for '" + expr + "', but was '" + value + "'" );
    }
  }

  public String toString()
  {
    return _lhs + (_not ? NotEquals.getToken() : Equals.getToken()) + _rhs;
  }
}
