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

public class SrcAnnotationArrayExpression extends SrcAnnotationExpression
{
  public SrcAnnotationArrayExpression( String fqn )
  {
    super( fqn );
  }

  public SrcAnnotationArrayExpression( Class type )
  {
    super( type.getName() );
  }

  public SrcAnnotationExpression copy()
  {
    SrcAnnotationArrayExpression copy = new SrcAnnotationArrayExpression( getAnnotationType() );
    for( SrcArgument expr : getArguments() )
    {
      copy.addArgument( expr.copy() );
    }
    return copy;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    indent( sb, indent );
    sb.append( '{' );
    for( int i = 0; i < getArguments().size(); i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      SrcArgument arg = getArguments().get( i );
      arg.render( sb, 0 );
    }
    sb.append( '}' );
    return sb;
  }
}
