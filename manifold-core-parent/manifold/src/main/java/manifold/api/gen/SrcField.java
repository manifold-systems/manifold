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
public class SrcField extends SrcAnnotated<SrcField>
{
  private SrcType _type;
  private SrcExpression _initializer;
  private boolean _enumConst;

  public SrcField( String name, Class type )
  {
    super();
    name( name );
    type( type );
  }

  public SrcField( String name, String type )
  {
    super();
    name( name );
    type( type );
  }

  public SrcField( String name, SrcType type )
  {
    super();
    name( name );
    type( type );
  }

  public SrcField( SrcClass srcClass )
  {
    super( srcClass );
  }

  public SrcField type( SrcType type )
  {
    _type = type;
    return this;
  }

  public SrcField type( Class type )
  {
    _type = new SrcType( type );
    return this;
  }

  public SrcField type( String type )
  {
    _type = new SrcType( type );
    return this;
  }

  public SrcField initializer( SrcExpression expr )
  {
    _initializer = expr;
    return this;
  }

  public boolean isEnumConst()
  {
    return _enumConst;
  }
  public SrcField enumConst()
  {
    _enumConst = true;
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, indent, false );
    indent( sb, indent );
    if( isEnumConst() )
    {
      sb.append( getSimpleName() ).append( ",\n" );
    }
    else
    {
      renderModifiers( sb, false, 0 );
      _type.render( sb, 0 ).append( ' ' ).append( getSimpleName() );
      if( _initializer != null )
      {
        sb.append( " = " );
        _initializer.render( sb, 0 ).append( ";\n" );
      }
      else
      {
        sb.append( ";\n" );
      }
    }
    return sb;
  }
}
