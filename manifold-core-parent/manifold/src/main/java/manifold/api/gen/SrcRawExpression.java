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
public class SrcRawExpression extends SrcExpression<SrcRawExpression>
{
  private String _text;

  public SrcRawExpression( String text )
  {
    _text = text;
  }

  public SrcRawExpression( Class type, Object value )
  {
    _text = makeCompileTimeConstantValue( new SrcType( type ), value );
  }

  public SrcRawExpression( SrcType type, Object value )
  {
    _text = makeCompileTimeConstantValue( type, value );
  }

  @Override
  public SrcRawExpression copy()
  {
    return new SrcRawExpression( _text );
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    indent( sb, indent );
    sb.append( _text );
    return sb;
  }

  public String toString()
  {
    return _text;
  }
}
