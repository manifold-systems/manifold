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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import manifold.api.json.DynamicType;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonStructureType;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class JsonUnionType extends JsonStructureType
{
  private Map<String, IJsonType> _constituentTypes;
  private LocklessLazyVar<JsonEnumType> _collapsedEnumType = LocklessLazyVar.make( () -> {
    if( !getMembers().isEmpty() )
    {
      return null;
    }
    if( getConstituents().stream().allMatch( e -> e instanceof JsonEnumType ) )
    {
      return makeEnumType( getConstituents() );
    }
    return null;
  } );

  public JsonUnionType( JsonSchemaType parent, URL source, String name )
  {
    super( parent, source, name );
    _constituentTypes = Collections.emptyMap();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();
    for( Map.Entry<String, IJsonType> entry: new HashSet<>( _constituentTypes.entrySet() ) )
    {
      IJsonType type = entry.getValue();
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        type = ((LazyRefJsonType)type).resolve();
        _constituentTypes.put( entry.getKey(), type );
      }
    }
  }

  public Collection<? extends IJsonType> getConstituents()
  {
    return _constituentTypes.values();
  }

  void addConstituent( String name, IJsonType type )
  {
    if( _constituentTypes.isEmpty() )
    {
      _constituentTypes = new HashMap<>();
    }
    _constituentTypes.put( name, type );
    if( type instanceof IJsonParentType && !isDefinition( type ) )
    {
      super.addChild( name, (IJsonParentType)type );
    }
  }

  private boolean isDefinition( IJsonType type )
  {
    return type.getParent() != null &&
           type.getParent().getName().equals( JsonSchemaTransformer.JSCH_DEFINITIONS );
  }

  public JsonUnionType merge( IJsonType type )
  {
    IJsonType mergedType = null;
    for( IJsonType c: getConstituents() )
    {
      mergedType = Json.mergeTypesNoUnion( c, type );
      if( mergedType != null && mergedType != DynamicType.instance() )
      {
        break;
      }
    }

    if( mergedType == null )
    {
      mergedType = type;
    }
    addConstituent( mergedType.getName(), mergedType );
    return this;
  }

  public JsonEnumType getCollapsedEnumType()
  {
    return _collapsedEnumType.get();
  }

  @Override
  public void render( StringBuilder sb, int indent, boolean mutable )
  {
    JsonEnumType collapsedEnumType = getCollapsedEnumType();
    if( collapsedEnumType != null )
    {
      collapsedEnumType.render( sb, indent, mutable );
      return;
    }
    super.render( sb, indent, mutable );
  }
}
