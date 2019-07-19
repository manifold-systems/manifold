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
import java.util.function.Predicate;
import manifold.preprocessor.definitions.Definitions;

public abstract class Expression
{
  private final List<ParseError> _errors = new ArrayList<>();
  private final int _startOffset;
  private final int _endOffset;

  public Expression( int startOffset, int endOffset )
  {
    _startOffset = startOffset;
    _endOffset = endOffset;
  }

  public abstract List<Expression> getChildren();

  public abstract boolean evaluate( Definitions definitions );

  public int getStartOffset()
  {
    return _startOffset;
  }

  public int getEndOffset()
  {
    return _endOffset;
  }

  void error( String message, int tokenStart )
  {
    _errors.add( new ParseError( message, tokenStart ) );
  }

  public boolean visitErrors( Predicate<ParseError> visitor )
  {
    for( ParseError e: _errors )
    {
      if( !visitor.test( e ) )
      {
        return false;
      }
    }
    for( Expression child: getChildren() )
    {
      if( !child.visitErrors( visitor ) )
      {
        return false;
      }
    }
    return true;
  }

  public boolean hasErrors()
  {
    return !visitErrors( e -> false );
  }
}
