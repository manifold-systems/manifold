package manifold.api.json.schema;

import java.util.List;
import javax.script.Bindings;
import manifold.api.json.DynamicType;
import manifold.api.json.ErrantType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonListType;
import manifold.api.json.JsonSchemaType;
import manifold.api.json.Token;
import manifold.internal.javac.IIssue;
import manifold.util.Pair;

/**
 */
class ArrayTransformer
{
  private static final String JSCH_ITEMS = "items";

  private final JsonSchemaTransformer _schemaTx;
  private final String _name;
  private final JsonListType _type;
  private final Bindings _jsonObj;

  static JsonListType transform( JsonSchemaTransformer schemaTx, String name, JsonListType type, Bindings jsonObj )
  {
    ArrayTransformer arrayTx = new ArrayTransformer( schemaTx, name, type, jsonObj );
    return arrayTx.transform();
  }

  private ArrayTransformer( JsonSchemaTransformer schemaTx, String name, JsonListType type, Bindings jsonObj )
  {
    _schemaTx = schemaTx;
    _name = name;
    _jsonObj = jsonObj;
    _type = type;
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
    _schemaTx.cacheByFqn( _type ); // must cache now to handle recursive refs

    assignComponentType();

    return _type;
  }

  private void assignComponentType()
  {
    IJsonType componentType = null;
    Object value = _jsonObj.get( JSCH_ITEMS );
    Object items;
    Token token = null;
    if( value instanceof Pair )
    {
      items = ((Pair)value).getSecond();
      token = (Token)((Pair)value).getFirst();
    }
    else
    {
      items = value;
    }
    if( items instanceof List )
    {
      for( Object elem : (List)items )
      {
        IJsonType csr = _schemaTx.transformType( _type, _type.getFile(), _name, (Bindings)elem );
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
    else if( items instanceof Bindings )
    {
      componentType = _schemaTx.transformType( _type, _type.getFile(), _name, (Bindings)items );
    }
    else
    {
      _type.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Expecting '{' or '[' for object or array to contain array component type" ) );
      componentType = new ErrantType( _type.getFile(), "Errant" );
    }
    _type.setComponentType( componentType );
  }
}
