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

public abstract class Statement
{
  private final TokenType _tokenType;
  private final int _tokenStart;
  private final int _tokenEnd;

  public Statement( TokenType tokenType, int start, int end )
  {
    _tokenType = tokenType;
    _tokenStart = start;
    _tokenEnd = end;
  }

  public TokenType getTokenType()
  {
    return _tokenType;
  }

  public int getTokenStart()
  {
    return _tokenStart;
  }

  public int getTokenEnd()
  {
    return _tokenEnd;
  }

  public abstract void execute( StringBuilder result, CharSequence source, boolean visible, Definitions definitions );
  public abstract void execute( List<SourceStatement> result, boolean visible, Definitions definitions );

  public abstract boolean hasPreprocessorDirectives();
}
