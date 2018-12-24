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
public class SrcAnnotationExpression extends SrcExpression<SrcAnnotationExpression>
{
  private String _fqn;
  private List<SrcArgument> _arguments = new ArrayList<>();

  public SrcAnnotationExpression( String fqn )
  {
    _fqn = fqn;
  }

  public SrcAnnotationExpression( Class type )
  {
    _fqn = type.getName();
  }

  public SrcAnnotationExpression addArgument( SrcArgument arg )
  {
    _arguments.add( arg );
    return this;
  }

  public SrcAnnotationExpression addArgument( String paramName, Class type, Object value )
  {
    _arguments.add( new SrcArgument( type, value ).name( paramName ) );
    return this;
  }

  public SrcAnnotationExpression addArgument( String paramName, SrcType type, Object value )
  {
    _arguments.add( new SrcArgument( type, value ).name( paramName ) );
    return this;
  }

  public SrcAnnotationExpression copy()
  {
    SrcAnnotationExpression copy = new SrcAnnotationExpression( _fqn );
    for( SrcArgument expr : _arguments )
    {
      copy.addArgument( expr.copy() );
    }
    return copy;
  }

  public String getAnnotationType()
  {
    return _fqn;
  }

  public List<SrcArgument> getArguments()
  {
    return _arguments;
  }
  public SrcArgument getArgument( String paramName )
  {
    for( SrcArgument arg: _arguments )
    {
      String argName = arg.getSimpleName();
      if( argName != null && !argName.isEmpty() )
      {
        if( paramName.equals( argName ) )
        {
          return arg;
        }
      }
      else if( paramName == null || paramName.isEmpty() || paramName.equals( "value" ) )
      {
        return arg;
      }
    }
    return null;
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    indent( sb, indent );
    sb.append( '@' ).append( _fqn );
    renderArgumenets( sb, _arguments, indent, sameLine );
    return sb;
  }
}
