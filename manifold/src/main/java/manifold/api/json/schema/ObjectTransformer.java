package manifold.api.json.schema;

import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonSchemaType;
import manifold.api.json.JsonStructureType;

/**
 */
class ObjectTransformer
{
  private final JsonSchemaTransformer _schemaTx;
  private final JsonStructureType _type;
  private final Bindings _jsonObj;

  static JsonStructureType transform( JsonSchemaTransformer schemaTx, String name, JsonSchemaType parent, Bindings jsonObj )
  {
    ObjectTransformer objectTx = new ObjectTransformer( schemaTx, name, parent, jsonObj );
    return objectTx.transform();
  }

  static JsonStructureType transform( JsonSchemaTransformer schemaTx, JsonStructureType type, Bindings jsonObj )
  {
    ObjectTransformer objectTx = new ObjectTransformer( schemaTx, type, jsonObj );
    return objectTx.transform();
  }

  private ObjectTransformer( JsonSchemaTransformer schemaTx, String name, JsonSchemaType parent, Bindings jsonObj )
  {
    _schemaTx = schemaTx;
    _jsonObj = jsonObj;
    _type = new JsonStructureType( parent, name );
  }

  private ObjectTransformer( JsonSchemaTransformer schemaTx, JsonStructureType type, Bindings jsonObj )
  {
    _schemaTx = schemaTx;
    _jsonObj = jsonObj;
    _type = type;
  }

  JsonStructureType getType()
  {
    return _type;
  }

  private JsonStructureType transform()
  {
    IJsonParentType parent = _type.getParent();
    if( parent != null )
    {
      parent.addChild( _type.getLabel(), _type );
    }
    _schemaTx.cache( _type ); // must cache now to handle recursive refs

    addProperties();

    return _type;
  }

  private void addProperties()
  {
    Bindings properties = (Bindings)_jsonObj.get( "properties" );
    if( properties == null )
    {
      return;
    }

    for( Map.Entry<String, Object> entry : properties.entrySet() )
    {
      String name = entry.getKey();
      Bindings value = (Bindings)entry.getValue();
      IJsonType type = _schemaTx.transformType( _type, name, value );
      _type.addMember( name, type );
    }
  }
}
