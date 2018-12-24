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
public class SrcMethodCallExpression extends SrcExpression<SrcMethodCallExpression>
{
  private List<SrcArgument> _arguments = new ArrayList<>();

  public SrcMethodCallExpression( String name )
  {
    name( name );
  }

  @Override
  public SrcMethodCallExpression copy()
  {
    SrcMethodCallExpression expr = new SrcMethodCallExpression( getSimpleName() );
    for( SrcArgument arg : _arguments )
    {
      expr.addArgument( arg.copy() );
    }
    return expr;
  }

  public List<SrcArgument> getArguments()
  {
    return _arguments;
  }

  public SrcMethodCallExpression addArgument( SrcArgument arg )
  {
    _arguments.add( arg );
    arg.setOwner( this );
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( getSimpleName() );
    renderArgumenets( sb, _arguments, indent, true );
    return sb;
  }
}
