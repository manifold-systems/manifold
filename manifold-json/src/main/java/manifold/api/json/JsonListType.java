package manifold.api.json;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class JsonListType extends JsonSchemaType
{
  private IJsonType _componentType;
  private Map<String, IJsonParentType> _innerTypes;

  public JsonListType( String label, URL source, JsonSchemaType parent )
  {
    super( label, source, parent );
    _innerTypes = new HashMap<>();
  }

  @Override
  public String getLabel()
  {
    return super.getName();
  }

  public String getName()
  {
    return "java.util.List<" + _componentType.getIdentifier() + ">";
  }

  public String getIdentifier()
  {
    return getName();
  }

  public void addChild( String name, IJsonParentType type )
  {
    _innerTypes.put( name, type );
  }

  public IJsonType findChild( String name )
  {
    IJsonType inner = _innerTypes.get( name );
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
      throw new IllegalStateException( "Component type already set to: " + _componentType.getIdentifier() + ", which is not the same as: " + compType.getIdentifier() );
    }
    _componentType = compType;
  }

  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _innerTypes;
  }

  IJsonType merge( JsonListType other )
  {
    JsonListType mergedType = new JsonListType( getLabel(), getFile(), getParent() );

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

    if( !mergeInnerTypes( other, mergedType, _innerTypes ) )
    {
      return null;
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
    return _innerTypes.equals( that._innerTypes );
  }

  @Override
  public int hashCode()
  {
    int result = _componentType.hashCode();
    result = 31 * result + _innerTypes.hashCode();
    return result;
  }
}
