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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonListType;
import manifold.util.JsonUtil;

/**
 * The base JSON Schema type.
 * <p>
 * <b>Attention!</b> subclasses must take care to share state with copies.
 * See the {@link State} class below.
 */
public abstract class JsonSchemaType implements IJsonParentType, Cloneable
{
  /**
   * Since we use clone() to copy, assignment to these fields must be reflected across all copies,
   * hence the encapsulation/indirection with the State class.
   */
  private static class State
  {
    private final String _name;
    private JsonSchemaType _parent;
    private final URL _file;
    private List<IJsonType> _definitions;
    private List<JsonIssue> _issues;
    private boolean _bSchemaType;
    private ResolveState _resolveState;

    private State( String name, JsonSchemaType parent, URL file )
    {
      _name = name;
      _parent = parent;
      _file = file;

      _issues = Collections.emptyList();
      _resolveState = ResolveState.Unresolved;
    }
  }

  enum ResolveState
  {
    Unresolved, Resolving, Resolved
  }

  private final State _state; // shared state across type copies
  private TypeAttributes _typeAttributes; // non-shared state that is different per type copy

  
  protected JsonSchemaType( String name, URL source, JsonSchemaType parent, TypeAttributes attr )
  {
    _state = new State( name, parent, source );
    _typeAttributes = attr;
  }

  public abstract String getFqn();

  final public void resolveRefs()
  {
    if( _state._resolveState != ResolveState.Unresolved )
    {
      return;
    }

    _state._resolveState = ResolveState.Resolving;
    try
    {
      resolveRefsImpl();
    }
    finally
    {
      _state._resolveState = ResolveState.Resolved;
    }
  }

  protected void resolveRefsImpl()
  {
    List<IJsonType> definitions = getDefinitions();
    if( definitions != null && !definitions.isEmpty() )
    {
      List<IJsonType> resolved = new ArrayList<>();
      for( IJsonType type : definitions )
      {
        if( type instanceof JsonSchemaType )
        {
          ((JsonSchemaType)type).resolveRefs();
        }
        else if( type instanceof LazyRefJsonType )
        {
          type = ((LazyRefJsonType)type).resolve();
        }
        resolved.add( type );
      }
      _state._definitions = resolved;
    }
  }

  protected boolean isParentRoot()
  {
    return getParent() == null ||
           getParent().getParent() == null && (getParent() instanceof JsonListType ||
                                               !getParent().getName().equals( JsonSchemaTransformer.JSCH_DEFINITIONS ));
  }

  public URL getFile()
  {
    return _state._file != null
           ? _state._file
           : _state._parent != null
             ? _state._parent.getFile()
             : null;
  }

  public String getLabel()
  {
    return getName();
  }

  @Override
  public String getName()
  {
    return _state._name;
  }

  @Override
  public String getIdentifier()
  {
    return JsonUtil.makeIdentifier( getName() );
  }

  @Override
  public JsonSchemaType getParent()
  {
    return _state._parent;
  }
  public void setParent( IJsonParentType parent )
  {
    _state._parent = (JsonSchemaType)parent;
  }

  @Override
  public List<IJsonType> getDefinitions()
  {
    return _state._definitions;
  }

  public void setDefinitions( List<IJsonType> definitions )
  {
    _state._definitions = definitions;
  }

  protected boolean isSchemaType()
  {
    return _state._bSchemaType;
  }

  protected void setJsonSchema()
  {
    _state._bSchemaType = true;
  }

  @Override
  public TypeAttributes getTypeAttributes()
  {
    return _typeAttributes;
  }
  @Override
  public JsonSchemaType copyWithAttributes( TypeAttributes attributes )
  {
    if( getTypeAttributes().equals( attributes ) )
    {
      return this;
    }

    try
    {
      JsonSchemaType copy = (JsonSchemaType)clone();
      copy._typeAttributes = copy._typeAttributes.overrideWith( attributes );
      return copy;
    }
    catch( CloneNotSupportedException e )
    {
      throw new RuntimeException( e );
    }
  }

  protected boolean mergeInnerTypes( IJsonParentType other, IJsonParentType mergedType, Map<String, IJsonParentType> innerTypes )
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
  public List<JsonIssue> getIssues()
  {
    if( getParent() != null )
    {
      return getParent().getIssues();
    }

    return _state._issues;
  }

  @Override
  public void addIssue( JsonIssue issue )
  {
    if( getParent() != null )
    {
      getParent().addIssue( issue );
      return;
    }

    if( _state._issues.isEmpty() )
    {
      _state._issues = new ArrayList<>();
    }
    _state._issues.add( issue );
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
