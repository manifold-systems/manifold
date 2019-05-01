/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SrcStatementBlock extends SrcStatement<SrcStatementBlock>
{
  private List<SrcStatement> _statements = new ArrayList<>();

  public SrcStatementBlock addStatement( SrcStatement stmt )
  {
    _statements.add( stmt );
    return this;
  }

  public SrcStatementBlock addStatement( String rawText )
  {
    _statements.add( new SrcRawStatement().rawText( rawText ) );
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, true );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    if( sameLine )
    {
      sb.append( " {\n" );
    }
    else
    {
      sb.append( indent( sb, indent ) ).append( "{\n" );
    }
    for( SrcStatement stmt : _statements )
    {
      stmt.render( sb, indent + INDENT );
    }
    sb.append( indent( sb, indent ) ).append( "}\n" );
    return sb;
  }
}
