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

public abstract class TerminalExpression extends Expression
{
  public TerminalExpression( int startOffset, int endOffset )
  {
    super( startOffset, endOffset );
  }

  @Override
  public List<Expression> getChildren()
  {
    return Collections.emptyList();
  }
}
