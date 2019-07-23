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

import java.util.Collections;
import java.util.List;
import manifold.preprocessor.definitions.Definitions;

public class ParenthesizedExpression extends Expression
{
  private Expression _expr;

  ParenthesizedExpression( Expression expr, int start, int end )
  {
    super( start, end );
    _expr = expr;
  }

  Expression getExpression()
  {
    return _expr;
  }

  @Override
  public List<Expression> getChildren()
  {
    return Collections.singletonList( _expr );
  }

  @Override
  public boolean evaluate( Definitions definitions )
  {
    return getExpression().evaluate( definitions );
  }

  public String getValue( Definitions definitions )
  {
    return getExpression().getValue( definitions );
  }

  @Override
  public String toString()
  {
    return "(" + _expr + ")";
  }
}