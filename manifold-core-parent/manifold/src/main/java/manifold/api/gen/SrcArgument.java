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
public class SrcArgument extends SrcAnnotated<SrcArgument>
{
  private SrcExpression _value;

  public SrcArgument( SrcExpression value )
  {
    _value = value;
    _value.setOwner( this );
  }

  public SrcArgument( Class type, Object value )
  {
    _value = new SrcRawExpression( type, value );
    _value.setOwner( this );
  }

  public SrcArgument( SrcType type, Object value )
  {
    _value = new SrcRawExpression( type, value );
    _value.setOwner( this );
  }

  public SrcArgument copy()
  {
    return new SrcArgument( _value.copy() ).name( getSimpleName() );
  }

  public SrcExpression getValue()
  {
    return _value;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, 0, true );
    if( getSimpleName() != null )
    {
      sb.append( getSimpleName() ).append( " = " );
    }
    _value.render( sb, 0 );
    return sb;
  }
}
