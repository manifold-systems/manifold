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

import manifold.preprocessor.definitions.Definitions;
import manifold.util.ManEscapeUtil;

//## todo: on the fence with this
public class StringLiteral extends TerminalExpression implements IValue
{
  private final String _value;

  StringLiteral( String value, int start, int end )
  {
    super( start, end );
    if( value == null )
    {
      throw new IllegalArgumentException( "Null value not allowed" );
    }
    _value = value;
  }

  @Override
  public boolean evaluate( Definitions definitions )
  {
    return true;
  }

  @Override
  public String getValue( Definitions definitions )
  {
    return _value;
  }

  public String toString()
  {
    return "\"" + ManEscapeUtil.escapeForJavaStringLiteral( _value ) + "\"";
  }
}
