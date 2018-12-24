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

/**
 */
public class SrcSwitchCase extends SrcStatement<SrcSwitchCase>
{
  private SrcExpression _expr;
  private SrcStatement _stmt;

  public SrcSwitchCase( Class type, Object valueExpr )
  {
    _expr = new SrcRawExpression( type, valueExpr );
  }

  public SrcSwitchCase( SrcType type, Object valueExpr )
  {
    _expr = new SrcRawExpression( type, valueExpr );
  }

  public SrcSwitchCase statement( SrcStatement stmt )
  {
    _stmt = stmt;
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( "case " ).append( _expr ).append( ":\n" );
    if( _stmt != null )
    {
      if( _stmt instanceof SrcStatementBlock )
      {
        ((SrcStatementBlock)_stmt).render( sb, indent, false );
      }
      else
      {
        _stmt.render( sb, indent + INDENT );
      }
    }
    return sb;
  }
}
