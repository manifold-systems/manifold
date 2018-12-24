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

import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonStructureType;
import manifold.api.json.Token;
import manifold.internal.javac.IIssue;
import manifold.util.Pair;

/**
 */
class ObjectTransformer
{
  private final JsonSchemaTransformer _schemaTx;
  private final JsonStructureType _type;
  private final Bindings _jsonObj;

  static void transform( JsonSchemaTransformer schemaTx, JsonStructureType type, Bindings jsonObj )
  {
    new ObjectTransformer( schemaTx, type, jsonObj ).transform();
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

  private void transform()
  {
    IJsonParentType parent = _type.getParent();
    if( parent != null )
    {
      parent.addChild( _type.getLabel(), _type );
    }
    _schemaTx.cacheByFqn( _type ); // must cache now to handle recursive refs

    addProperties();
  }

  private void addProperties()
  {
    Object props = _jsonObj.get( JsonSchemaTransformer.JSCH_PROPERTIES );
    if( props == null )
    {
      return;
    }

    Token token = null;
    try
    {
      Bindings properties;
      if( props instanceof Pair )
      {
        properties = (Bindings)((Pair)props).getSecond();
      }
      else
      {
        properties = (Bindings)props;
      }

      for( Map.Entry<String, Object> entry : properties.entrySet() )
      {
        String name = entry.getKey();
        Object value = entry.getValue();
        Bindings bindings;
        if( value instanceof Pair )
        {
          token = ((Token[])((Pair)value).getFirst())[0];
          bindings = (Bindings)((Pair)value).getSecond();
        }
        else
        {
          token = null;
          bindings = (Bindings)value;
        }

        IJsonType type = _schemaTx.transformType( _type, _type.getFile(), name, bindings );
        _type.addMember( name, type, token );
      }
    }
    catch( Exception e )
    {
      _type.addIssue( new JsonIssue( IIssue.Kind.Error, token, e.getMessage() ) );
    }
  }
}
