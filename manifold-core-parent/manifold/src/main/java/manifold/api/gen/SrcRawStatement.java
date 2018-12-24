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
public class SrcRawStatement extends SrcStatement<SrcRawStatement>
{
  private List<String> _text = new ArrayList<>();

  public SrcRawStatement()
  {
    super();
  }

  public SrcRawStatement( SrcStatementBlock owner )
  {
    super( owner );
  }

  public SrcRawStatement rawText( String text )
  {
    _text.add( text );
    return this;
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    for( String text : _text )
    {
      indent( sb, indent );
      sb.append( text ).append( "\n" );
    }
    return sb;
  }
}
