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

package manifold.api.json.schema;

import java.util.List;
import javax.script.Bindings;
import manifold.api.json.DynamicType;
import manifold.api.json.ErrantType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonListType;
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
    assignComponentType();
    if( parent != null )
    {
      _schemaTx.cacheSimpleByFqn( parent, _type.getLabel(), _type );
    }
    else
    {
      _schemaTx.cacheByFqn( _type );
    }

    return _type;
  }

  private void assignComponentType()
  {
    IJsonType componentType = null;
    Object value = _jsonObj.get( JSCH_ITEMS );
    Object items;
    Token[] tokens = null;
    if( value instanceof Pair )
    {
      items = ((Pair)value).getSecond();
      tokens = (Token[])((Pair)value).getFirst();
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
    else if( items == null )
    {
      componentType = DynamicType.instance();
    }
    else
    {
      _type.addIssue( new JsonIssue( IIssue.Kind.Error, tokens != null ? tokens[1] : null, "Expecting '{' or '[' for object or array to contain array component type" ) );
      componentType = new ErrantType( _type.getFile(), "Errant" );
    }
    _type.setComponentType( componentType );
  }
}
