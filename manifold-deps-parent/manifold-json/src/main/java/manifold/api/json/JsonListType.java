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

package manifold.api.json;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import manifold.api.json.schema.JsonSchemaType;
import manifold.api.json.schema.JsonUnionType;
import manifold.api.json.schema.LazyRefJsonType;
import manifold.api.json.schema.TypeAttributes;

/**
 */
public class JsonListType extends JsonSchemaType
{
  private static final class State
  {
    private IJsonType _componentType;
    private Map<String, IJsonParentType> _innerTypes;
  }

  private final State _state;


  public JsonListType( String label, URL source, JsonSchemaType parent, TypeAttributes attr )
  {
    super( label, source, parent, attr );
    _state = new State();
    _state._innerTypes = Collections.emptyMap();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();
    if( _state._componentType instanceof JsonSchemaType )
    {
      ((JsonSchemaType)_state._componentType).resolveRefs();
    }
    else if( _state._componentType instanceof LazyRefJsonType )
    {
      _state._componentType = ((LazyRefJsonType)_state._componentType).resolve();
    }
    for( Map.Entry<String, IJsonParentType> entry : new HashSet<>( _state._innerTypes.entrySet() ) )
    {
      IJsonType type = entry.getValue();
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        _state._innerTypes.put( entry.getKey(), (IJsonParentType)((LazyRefJsonType)type).resolve() );
      }
    }
  }

  @Override
  public String getLabel()
  {
    return super.getName();
  }

  public String getName()
  {
    return "ListOf" + getComponentTypeName();
  }

  private String getComponentTypeName()
  {
    if( _state._componentType == null || _state._componentType instanceof LazyRefJsonType )
    {
      // can happen if asked before this list type is fully configured
      return "_undefined_";
    }

    return _state._componentType instanceof JsonUnionType ? "Object" : _state._componentType.getIdentifier();
  }

  public String getIdentifier()
  {
    return getName();
  }

  @Override
  public String getFqn()
  {
    return getName();
  }

  public void addChild( String name, IJsonParentType type )
  {
    if( _state._innerTypes.isEmpty() )
    {
      _state._innerTypes = new HashMap<>();
    }
    _state._innerTypes.put( name, type );
  }

  public IJsonType findChild( String name )
  {
    IJsonType innerType = _state._innerTypes.get( name );
    if( innerType == null )
    {
      if( _state._componentType instanceof IJsonParentType )
      {
        innerType = ((IJsonParentType)_state._componentType).findChild( name );
      }
      if( innerType == null )
      {
        List<IJsonType> definitions = getDefinitions();
        if( definitions != null )
        {
          for( IJsonType child : definitions )
          {
            if( child.getName().equals( name ) )
            {
              innerType = child;
              break;
            }
          }
        }
      }
    }
    return innerType;
  }

  @SuppressWarnings("WeakerAccess")
  public IJsonType getComponentType()
  {
    return _state._componentType;
  }

  public void setComponentType( IJsonType compType )
  {
    if( _state._componentType != null && _state._componentType != compType )
    {
      throw new IllegalStateException( "Component type already set to: " + _state._componentType.getIdentifier() + ", which is not the same as: " + compType.getIdentifier() );
    }
    _state._componentType = compType;
  }

  @SuppressWarnings("unused")
  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _state._innerTypes;
  }

  public JsonListType merge( IJsonType that )
  {
    if( !(that instanceof JsonListType) )
    {
      return null;
    }

    JsonListType other = (JsonListType)that;
    JsonListType mergedType = new JsonListType( getLabel(), getFile(), getParent(), getTypeAttributes().blendWith( that.getTypeAttributes() ) );

    if( !getComponentType().equalsStructurally( other.getComponentType() ) )
    {
      IJsonType componentType = Json.mergeTypes( getComponentType(), other.getComponentType() );
      if( componentType != null )
      {
        mergedType.setComponentType( componentType );
      }
      else
      {
        return null;
      }
    }

    if( !mergeInnerTypes( other, mergedType, _state._innerTypes ) )
    {
      return null;
    }

    return mergedType;
  }

  public void render( StringBuilder sb, int indent, boolean mutable )
  {
    for( IJsonParentType child : _state._innerTypes.values() )
    {
      child.render( sb, indent, mutable );
    }
  }

  @Override
  public boolean equalsStructurally( IJsonType o )
  {
    if( this == o )
    {
      return true;
    }

    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    JsonListType that = (JsonListType)o;

    if( !_state._componentType.equalsStructurally( that._state._componentType ) )
    {
      return false;
    }
    return _state._innerTypes.size() == that._state._innerTypes.size() &&
           _state._innerTypes.keySet().stream().allMatch(
             key -> that._state._innerTypes.containsKey( key ) &&
                    _state._innerTypes.get( key ).equalsStructurally( that._state._innerTypes.get( key ) ) );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }

    if( isSchemaType() )
    {
      // Json Schema types must be identity compared
      return false;
    }

    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    JsonListType that = (JsonListType)o;

    if( !_state._componentType.equals( that._state._componentType ) )
    {
      return false;
    }
    return _state._innerTypes.equals( that._state._innerTypes );
  }

  @Override
  public int hashCode()
  {
    int result = _state._componentType.hashCode();
    result = 31 * result + _state._innerTypes.hashCode();
    return result;
  }
}
