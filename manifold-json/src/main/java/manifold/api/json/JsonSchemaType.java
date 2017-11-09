package manifold.api.json;

import java.net.URL;
import java.util.List;
import java.util.Map;
import manifold.util.JsonUtil;

/**
 */
public abstract class JsonSchemaType implements IJsonParentType
{
  private final String _name;
  private final JsonSchemaType _parent;
  private List<IJsonType> _definitions;
  private URL _file;

  JsonSchemaType( String name, JsonSchemaType parent )
  {
    _name = name;
    _parent = parent;
  }

  public URL getFile()
  {
    return _file != null ? _file : _parent.getFile();
  }
  public void setFile( URL file )
  {
    _file = file;
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

  boolean mergeInnerTypes( IJsonParentType other, IJsonParentType mergedType, Map<String, IJsonParentType> innerTypes )
  {
    for( Map.Entry<String, IJsonParentType> e : innerTypes.entrySet() )
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
        return false;
      }
    }
    return true;
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
