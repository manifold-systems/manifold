package manifold.api.json;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class JsonListType extends JsonSchemaType
{
  private IJsonType _componentType;
  private Map<String, IJsonParentType> _innerTypes;

  public JsonListType( String label, JsonSchemaType parent )
  {
    super( label, parent );
    _innerTypes = new HashMap<>();
  }

  @Override
  public String getLabel()
  {
    return super.getName();
  }

  public String getName()
  {
    return "java.util.List<" + _componentType.getName() + ">";
  }

  public void addChild( String name, IJsonParentType type )
  {
    _innerTypes.put( name, type );
  }

  public IJsonParentType findChild( String name )
  {
    IJsonParentType inner = _innerTypes.get( name );
    if( inner == null && _componentType instanceof IJsonParentType )
    {
      inner = ((IJsonParentType)_componentType).findChild( name );
    }
    return inner;
  }

  public IJsonType getComponentType()
  {
    return _componentType;
  }

  public void setComponentType( IJsonType compType )
  {
    if( _componentType != null && _componentType != compType )
    {
      throw new IllegalStateException( "Component type already set to: " + _componentType.getName() + ", which is not the same as: " + compType.getName() );
    }
    _componentType = compType;
  }

  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _innerTypes;
  }

  public IJsonType merge( JsonListType other )
  {
    JsonListType mergedType = new JsonListType( getLabel(), getParent() );

    if( !getComponentType().equals( other.getComponentType() ) )
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

    for( Map.Entry<String, IJsonParentType> e : _innerTypes.entrySet() )
    {
      String name = e.getKey();
      IJsonType innerType = other.findChild( name );
      if( innerType != null )
      {
        innerType = Json.mergeTypes( e.getValue(), innerType );
      }
      else
      {
        innerType = e.getValue();
      }

      if( innerType != null )
      {
        mergedType.addChild( name, (IJsonParentType)innerType );
      }
      else
      {
        return null;
      }
    }

    return mergedType;
  }

  public void render( StringBuilder sb, int indent, boolean mutable )
  {
    for( IJsonParentType child : _innerTypes.values() )
    {
      child.render( sb, indent, mutable );
    }
  }

  @Override
  public boolean equals( Object o )
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

    if( !_componentType.equals( that._componentType ) )
    {
      return false;
    }
    if( !_innerTypes.equals( that._innerTypes ) )
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _componentType.hashCode();
    result = 31 * result + _innerTypes.hashCode();
    return result;
  }
}
