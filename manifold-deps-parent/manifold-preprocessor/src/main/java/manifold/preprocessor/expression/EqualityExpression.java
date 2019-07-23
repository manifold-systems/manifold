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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import manifold.preprocessor.definitions.Definitions;

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
    return _not != (_lhs.evaluate( definitions ) == _rhs.evaluate( definitions ) &&
                    Objects.equals( _lhs.getValue( definitions ), _rhs.getValue( definitions ) ));
  }

  public String toString()
  {
    return _lhs + " == " + _rhs;
  }
}
