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

package manifold.preprocessor.statement;

import java.util.List;
import manifold.preprocessor.definitions.Definitions;
import manifold.preprocessor.TokenType;
import manifold.preprocessor.expression.EmptyExpression;
import manifold.preprocessor.expression.Expression;

public class IfStatement extends Statement
{
  private final Expression _expr;
  private final List<Statement> _ifBlock;
  private final List<IfStatement> _elifs;
  private final List<Statement> _elseBlock;

  public IfStatement( TokenType tokenType, int start, int end, Expression expr,
                      List<Statement> ifBlock, List<IfStatement> elifs, List<Statement> elseBlock )
  {
    super( tokenType, start, end );
    _expr = expr;
    _ifBlock = ifBlock;
    _elifs = elifs;
    _elseBlock = elseBlock;
  }

  @Override
  public void execute( StringBuilder result, CharSequence source, boolean visible, Definitions definitions )
  {
    boolean ifCond = visible && evalExpr( definitions );
    for( Statement stmt: _ifBlock )
    {
      stmt.execute( result, source, ifCond, definitions );
    }

    boolean elifPassed = false;
    for( IfStatement elif: _elifs )
    {
      boolean elifCond = visible && !elifPassed && !ifCond && elif.evalExpr( definitions );
      elifPassed = elifPassed || elifCond;
      elif.execute( result, source, elifCond, definitions );
    }

    for( Statement stmt: _elseBlock )
    {
      stmt.execute( result, source, visible && !ifCond && !elifPassed, definitions );
    }
  }

  @Override
  public void execute( List<SourceStatement> result, boolean visible, Definitions definitions )
  {
    if( !visible )
    {
      return;
    }

    boolean ifCond = evalExpr( definitions );
    for( Statement stmt: _ifBlock )
    {
      stmt.execute( result, ifCond, definitions );
    }

    boolean elifPassed = false;
    for( IfStatement elif: _elifs )
    {
      boolean elifCond = !elifPassed && !ifCond && elif.evalExpr( definitions );
      elifPassed = elifPassed || elifCond;
      elif.execute( result, elifCond, definitions );
    }

    for( Statement stmt: _elseBlock )
    {
      stmt.execute( result, !ifCond && !elifPassed, definitions );
    }
  }

  @Override
  public boolean hasPreprocessorDirectives()
  {
    return true;
  }

  private boolean evalExpr( Definitions definitions )
  {
    return !(_expr instanceof EmptyExpression) && !_expr.hasErrors() && _expr.evaluate( definitions );
  }
}
