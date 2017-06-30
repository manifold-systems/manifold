package manifold.api.json.schema;

import java.util.List;
import javax.script.Bindings;
import manifold.api.json.DynamicType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonListType;
import manifold.api.json.JsonSchemaType;

/**
 */
class ArrayTransformer
{
  private static final String JSCH_ITEMS = "items";

  private final JsonSchemaTransformer _schemaTx;
  private final String _name;
  private final JsonListType _type;
  private final Bindings _jsonObj;

  static JsonListType transform( JsonSchemaTransformer schemaTx, String name, JsonSchemaType parent, Bindings jsonObj )
  {
    ArrayTransformer arrayTx = new ArrayTransformer( schemaTx, name, parent, jsonObj );
    return arrayTx.transform();
  }

  private ArrayTransformer( JsonSchemaTransformer schemaTx, String name, JsonSchemaType parent, Bindings jsonObj )
  {
    _schemaTx = schemaTx;
    _name = name;
    _jsonObj = jsonObj;
    _type = new JsonListType( name, parent );
  }

  JsonListType getType()
  {
    return _type;
  }

  private JsonListType transform()
  {
    JsonSchemaType parent = _type.getParent();
    if( parent != null )
    {
      parent.addChild( _type.getLabel(), _type );
    }
    _schemaTx.cache( _type ); // must cache now to handle recursive refs

    assignComponentType();

    return _type;
  }

  private void assignComponentType()
  {
    IJsonType componentType = null;
    Object items = _jsonObj.get( JSCH_ITEMS );
    if( items instanceof List )
    {
      for( Object elem : (List)items )
      {
        IJsonType csr = _schemaTx.transformType( _type, _name, (Bindings)elem );
        if( componentType == null )
        {
          componentType = csr;
        }
        else if( !csr.equals( componentType ) )
        {
          componentType = DynamicType.instance();
          break;
        }
      }
    }
    else
    {
      componentType = _schemaTx.transformType( _type, _name, (Bindings)items );
    }
    _type.setComponentType( componentType );
  }
}
