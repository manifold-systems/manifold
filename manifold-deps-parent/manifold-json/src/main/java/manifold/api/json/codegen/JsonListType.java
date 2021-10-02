/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.json.codegen;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.json.AbstractJsonTypeManifold;
import manifold.api.json.JsonTransformer;
import manifold.api.json.codegen.schema.JsonSchemaType;
import manifold.api.json.codegen.schema.JsonUnionType;
import manifold.api.json.codegen.schema.LazyRefJsonType;
import manifold.api.json.codegen.schema.TypeAttributes;
import manifold.ext.rt.RuntimeMethods;

/**
 *
 */
public class JsonListType extends JsonSchemaType
{
  private static final class State
  {
    private IJsonType _componentType;
    private Map<String, IJsonParentType> _innerTypes;
  }

  private final State _state;


  public JsonListType( String label, IFile source, JsonSchemaType parent, TypeAttributes attr )
  {
    super( label, source, parent, attr );
    _state = new State();
    _state._innerTypes = Collections.emptyMap();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();
    if( _state._componentType instanceof JsonSchemaType )
    {
      ((JsonSchemaType)_state._componentType).resolveRefs();
    }
    else if( _state._componentType instanceof LazyRefJsonType )
    {
      _state._componentType = ((LazyRefJsonType)_state._componentType).resolve();
    }
    for( Map.Entry<String, IJsonParentType> entry: new HashSet<>( _state._innerTypes.entrySet() ) )
    {
      IJsonType type = entry.getValue();
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        _state._innerTypes.put( entry.getKey(), (IJsonParentType)((LazyRefJsonType)type).resolve() );
      }
    }
  }

  public void addChild( String name, IJsonParentType type )
  {
    if( _state._innerTypes.isEmpty() )
    {
      _state._innerTypes = new LinkedHashMap<>();
    }
    _state._innerTypes.put( name, type );
  }

  public IJsonType findChild( String name )
  {
    IJsonType innerType = _state._innerTypes.get( name );
    if( innerType == null )
    {
      if( _state._componentType instanceof IJsonParentType )
      {
        innerType = ((IJsonParentType)_state._componentType).findChild( name );
      }
      if( innerType == null )
      {
        List<IJsonType> definitions = getDefinitions();
        if( definitions != null )
        {
          for( IJsonType child: definitions )
          {
            if( child.getName().equals( name ) )
            {
              innerType = child;
              break;
            }
          }
        }
      }
    }
    return innerType;
  }

  public IJsonType getComponentType()
  {
    return _state._componentType;
  }

  public void setComponentType( IJsonType compType )
  {
    if( _state._componentType != null && _state._componentType != compType )
    {
      throw new IllegalStateException( "Component type already set to: " + _state._componentType.getIdentifier() + ", which is not the same as: " + compType.getIdentifier() );
    }
    _state._componentType = compType;
  }

  @SuppressWarnings("unused")
  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _state._innerTypes;
  }

  public JsonListType merge( IJsonType that )
  {
    if( !(that instanceof JsonListType) )
    {
      return null;
    }

    JsonListType other = (JsonListType)that;
    JsonListType mergedType = new JsonListType( getLabel(), getFile(), getParent(), getTypeAttributes().blendWith( that.getTypeAttributes() ) );

    if( !getComponentType().equalsStructurally( other.getComponentType() ) )
    {
      IJsonType componentType = JsonTransformer.mergeTypes( getComponentType(), other.getComponentType() );
      if( componentType != null )
      {
        mergedType.setComponentType( componentType );
      }
      else
      {
        return null;
      }
    }

    if( !mergeInnerTypes( other, mergedType, _state._innerTypes ) )
    {
      return null;
    }

    return mergedType;
  }

  public void render( AbstractJsonTypeManifold tm, StringBuilder sb, int indent, boolean mutable )
  {
    setTm( tm );

    String name = getName();
    String identifier = addActualNameAnnotation( sb, indent, name, false );

    if( !(getParent() instanceof JsonStructureType) ||
        !((JsonStructureType)getParent()).addSourcePositionAnnotation( sb, indent, identifier ) )
    {
      if( getToken() != null )
      {
        // this is most likely a "definitions" inner class
        addSourcePositionAnnotation( sb, indent, identifier, getToken() );
      }
    }
    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "@Structural\n" );
    indent( sb, indent );
    //noinspection unused
    String componentType = makeTypeParameter( getComponentType(), true, false );
    sb.append( "public interface " ).append( identifier ).append( " extends IJsonList<$componentType> {\n" );
    renderFileField( sb, indent + 2 );
    renderStaticMembers( sb, indent + 2 );
    renderUnionAccessors( sb, indent + 2 );
    renderInnerTypes( sb, indent, mutable );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void renderUnionAccessors( StringBuilder sb, int indent )
  {
    IJsonType componentType = getComponentType();
    if( !(componentType instanceof JsonUnionType) )
    {
      return;
    }

    if( isCollapsedUnionEnum( componentType ) )
    {
      return;
    }

    for( IJsonType constituentType: getConstituents( componentType, new LinkedHashSet<>() ) )
    {
      sb.append( '\n' );
      String specificPropertyType = getConstituentQn( constituentType, componentType );
//      addSourcePositionAnnotation( sb, indent + 2, key );
      if( constituentType instanceof JsonSchemaType )
      {
        addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
      }
      indent( sb, indent + 2 );
      //noinspection unused
      String unionName = makeMemberIdentifier( constituentType );
      sb.append( "default $specificPropertyType getAs$unionName(int index) {\n" );
      indent( sb, indent + 4 );
      if( constituentType instanceof JsonListType || specificPropertyType.indexOf( '>' ) > 0 )
      {
        sb.append( "return ($specificPropertyType)getList().get(index);\n" );
      }
      else
      {
        //noinspection unused
        String rawSpecificPropertyType = removeGenerics( specificPropertyType );
        sb.append( "return ($specificPropertyType)" ).append( RuntimeMethods.class.getSimpleName() )
          .append( ".coerce(getList().get(index), $rawSpecificPropertyType.class);\n" );
      }
      indent( sb, indent + 2 );
      sb.append( "}\n" );

      //noinspection UnusedAssignment
      specificPropertyType = getConstituentQn( constituentType, componentType, true );
      if( constituentType instanceof JsonSchemaType )
      {
        addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
      }
      indent( sb, indent + 2 );
      sb.append( "default void setAs$unionName(int index, $specificPropertyType ${'$'}value) {\n" );
      indent( sb, indent + 4 );
      sb.append( "getList().set(index, " ).append( RuntimeMethods.class.getSimpleName() )
        .append( ".coerceToBindingValue(${'$'}value));\n" );
      indent( sb, indent + 2 );
      sb.append( "}\n" );

      if( constituentType instanceof JsonSchemaType )
      {
        addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
      }
      indent( sb, indent + 2 );
      sb.append( "default void addAs$unionName(int index, $specificPropertyType ${'$'}value) {\n" );
      indent( sb, indent + 4 );
      sb.append( "getList().add(index, " ).append( RuntimeMethods.class.getSimpleName() )
        .append( ".coerceToBindingValue(${'$'}value));\n" );
      indent( sb, indent + 2 );
      sb.append( "}\n" );
    }
  }

  private Set<IJsonType> getConstituents( IJsonType type, Set<IJsonType> constituents )
  {
    if( type instanceof JsonUnionType )
    {
      constituents.addAll( ((JsonUnionType)type).getConstituents() );
    }
    else if( type instanceof JsonListType )
    {
      getConstituents( ((JsonListType)type).getComponentType(), constituents );
    }
    return constituents;
  }

  private void renderStaticMembers( StringBuilder sb, int indent )
  {
    String typeName = getIdentifier();

    // Add a static create(...) method having parameters corresponding with "required" properties not having
    // a "default" value.
    addCreateMethod( sb, indent, typeName );

    // Provide a loader(...) method, returns Loader<typeName> with methods for loading content from String, URL, file, etc.
    addLoadMethod( sb, indent, typeName );

    // Provide a requester(urlBase) method, returns Requester<typeName> with for performing HTTP requests using HTTP GET, POST, PUT, PATCH, & DELETE
    addRequestMethods( sb, indent, typeName );

    // Allow non-schema files to load from themselves easily, also corresponds with @FragmentValue added to toplevel class
    addFromSourceMethod( sb, indent );
  }

  private void addLoadMethod( StringBuilder sb, int indent, @SuppressWarnings("unused") String typeName )
  {
    indent( sb, indent );
    //noinspection unused
    sb.append( "static " ).append( "Loader<$typeName>" ).append( " load() {\n" );
    indent( sb, indent );
    sb.append( "  return new Loader<>();\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addCreateMethod( StringBuilder sb, int indent, String typeName )
  {
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " create() {\n" );
    indent( sb, indent );
    sb.append( "  return ($typeName)new ArrayList();\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void renderInnerTypes( StringBuilder sb, int indent, boolean mutable )
  {
    renderJsonInnerTypes( sb, indent, mutable );
  }

  private void renderJsonInnerTypes( StringBuilder sb, int indent, boolean mutable )
  {
    for( IJsonParentType child: _state._innerTypes.values() )
    {
      child.renderInner( getTm(), sb, indent + 2, mutable );
    }
    List<IJsonType> definitions = getDefinitions();
    if( definitions != null )
    {
      for( IJsonType child: definitions )
      {
        if( child instanceof IJsonParentType )
        {
          ((IJsonParentType)child).renderInner( getTm(), sb, indent + 2, mutable );
        }
      }
    }
  }

  @Override
  public boolean equalsStructurally( IJsonType o )
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

    if( _state._componentType == null ||
        !_state._componentType.equalsStructurally( that._state._componentType ) )
    {
      return false;
    }
    return _state._innerTypes.size() == that._state._innerTypes.size() &&
           _state._innerTypes.keySet().stream().allMatch(
             key -> that._state._innerTypes.containsKey( key ) &&
                    _state._innerTypes.get( key ).equalsStructurally( that._state._innerTypes.get( key ) ) );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }

    if( isSchemaType() )
    {
      // Json Schema types must be identity compared
      return false;
    }

    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    JsonListType that = (JsonListType)o;

    if( _state._componentType == null ||
        !_state._componentType.equals( that._state._componentType ) )
    {
      return false;
    }
    return _state._innerTypes.equals( that._state._innerTypes );
  }

  @Override
  public int hashCode()
  {
    int result = _state._componentType == null ? 0 : _state._componentType.hashCode();
    result = 31 * result + _state._innerTypes.hashCode();
    return result;
  }
}
