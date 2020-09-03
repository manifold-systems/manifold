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

public class DefineStatement extends Statement
{
  private final String _name;

  public DefineStatement( int start, int end, String name )
  {
    super( TokenType.Define, start, end );
    _name = name;
  }

  @Override
  public void execute( StringBuilder result, CharSequence source, boolean visible, Definitions definitions )
  {
    preserveMaskedOutSpace( result, source );

    if( !visible )
    {
      return;
    }

    if( !_name.isEmpty() )
    {
      definitions.define( _name );
    }
  }

  @Override
  public void execute( List<SourceStatement> result, boolean visible, Definitions definitions )
  {
    if( visible && !_name.isEmpty() )
    {
      definitions.define( _name );
    }
  }

  @Override
  public boolean hasPreprocessorDirectives()
  {
    return true;
  }
}
