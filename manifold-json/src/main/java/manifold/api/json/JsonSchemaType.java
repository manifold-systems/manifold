package manifold.api.json;

import java.util.List;
import manifold.util.JsonUtil;

/**
 */
public abstract class JsonSchemaType implements IJsonParentType
{
  private final String _name;
  private final JsonSchemaType _parent;
  private List<IJsonType> _definitions;

  JsonSchemaType( String name, JsonSchemaType parent )
  {
    _name = name;
    _parent = parent;
  }

  public String getLabel()
  {
    return getName();
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getIdentifier()
  {
    return JsonUtil.makeIdentifier( getName() );
  }

  @Override
  public JsonSchemaType getParent()
  {
    return _parent;
  }

  @Override
  public List<IJsonType> getDefinitions()
  {
    return _definitions;
  }

  public void setDefinitions( List<IJsonType> definitions )
  {
    _definitions = definitions;
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

    JsonSchemaType that = (JsonSchemaType)o;

    return getName().equals( that.getName() );
  }

  @Override
  public int hashCode()
  {
    return getName().hashCode();
  }
}
