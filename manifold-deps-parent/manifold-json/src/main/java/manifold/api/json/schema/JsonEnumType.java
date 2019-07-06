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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcElement;
import manifold.api.gen.SrcType;
import manifold.api.json.AbstractJsonTypeManifold;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonBasicType;
import manifold.api.json.JsonStructureType;
import manifold.api.json.Token;
import manifold.ext.api.IBindingType;
import manifold.util.JsonUtil;
import manifold.util.Pair;

/**
 * Transform JSON Schema enum to Java enum:
 * <pre>
 * "enum": ["blue", "green", 5, 4.0]
 *
 * enum Foo implements IBindingType {
 *   blue("blue"),
 *   green("green"),
 *   _5(5),
 *   _4_0(4.0);
 *
 *   private final Object _value;
 *
 *   Foo(Object value) {
 *     _value = value;
 *   }
 *
 *   {@literal @}Override
 *   public Object toBindingValue() {
 *     return _value;
 *   }
 * }
 * </pre>
 * When calling {@code myObj.setFoo(MyObj.Foo.blue)} the JSON manifold marshals the <i>value</i> corresponding with
 * the Java enum const so that the JSON bindings always contains JSON values.  Similarly upon calling
 * {@code myObj.getFoo()} the value returned to Java code is always the {@code Foo enum} const corresponding with the
 * underlying bindings value.
 */
public class JsonEnumType extends JsonStructureType
{
  private final List<Object> _enumValues;
  private final boolean _hasNull;

  JsonEnumType( JsonSchemaType parent, IFile source, String name, List<?> list, TypeAttributes attr )
  {
    super( parent, source, name, attr );

    _enumValues = new ArrayList<>();
    boolean hasNull = false;
    for( Object value: list )
    {
      Token token;
      if( value instanceof Pair )
      {
        token = ((Token[])((Pair)value).getFirst())[0];
        value = ((Pair)value).getSecond();
      }
      else
      {
        token = null;
      }

      JsonBasicType type = JsonBasicType.get( value );
      if( !hasNull && type == null || type.getJsonType() == Type.Null )
      {
        hasNull = true;
        continue;
      }
      addMember( JsonUtil.makeIdentifier( String.valueOf( value ) ), type, token );
      _enumValues.add( value );
    }
    _hasNull = hasNull;
  }

  public JsonEnumType( JsonEnumType enum1, JsonEnumType enum2, JsonSchemaType parent, String name )
  {
    super( parent, enum1.getFile(), name, enum2 == null ? enum1.getTypeAttributes() : enum1.getTypeAttributes().blendWith( enum2.getTypeAttributes() ) );

    Map<String, IJsonType> members = new LinkedHashMap<>( enum1.getMembers() );
    Map<String, Token> memberLocations = new LinkedHashMap<>( enum1.getMemberLocations() );
    Set<Object> enumValues = new LinkedHashSet<>( enum1._enumValues );
    if( enum2 != null )
    {
      members.putAll( enum2.getMembers() );
      memberLocations.putAll( enum2.getMemberLocations() );
      enumValues.addAll( enum2._enumValues );
    }
    members.forEach( (m, v) -> addMember( m, v, memberLocations.get( m ) ) );
    _enumValues = new ArrayList<>( enumValues );
    _hasNull = enum1._hasNull || enum2 != null && enum2._hasNull;
    getTypeAttributes().setNullable( TypeAttributes.or( _hasNull,
                                     (TypeAttributes.and( enum1.getTypeAttributes().getNullable(),
                                       enum2 == null ? null : enum2.getTypeAttributes().getNullable() )) ) );
  }

  @Override
  public IJsonType merge( IJsonType that )
  {
    if( !getName().equals( that.getName() ) )
    {
      return null;
    }

    if( that instanceof JsonBasicType )
    {
      return that;
    }
    
    if( !(that instanceof JsonEnumType) )
    {
      return null;
    }

    return new JsonEnumType( this, (JsonEnumType)that, getParent(), getName() );
  }

  @Override
  public void render( AbstractJsonTypeManifold tm, StringBuilder sb, int indent, boolean mutable )
  {
    setTm( tm );

    if( getParent() != null )
    {
      sb.append( '\n' );
    }

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

    indent( sb, indent );
    sb.append( "public enum " ).append( identifier ).append( " implements " ).append( IBindingType.class.getTypeName() ).append( " {\n" );
    int i = 0;

    // Enum constants with values
    for( String key: getMembers().keySet() )
    {
      addSourcePositionAnnotation( sb, indent + 2, key );
      indent( sb, indent + 2 );
      Object objValue = _enumValues.get( i );
      sb.append( key ).append( "(" ).append( objValue == null ? "null" : SrcElement.makeCompileTimeConstantValue( new SrcType( objValue.getClass() ), objValue ) ).append( ")" );
      if( ++i == getMembers().size() )
      {
        sb.append( ";\n\n" );
      }
      else
      {
        sb.append( ",\n" );
      }
    }

    // static URL field
    renderFileField( sb, indent + 2, "static final" );

    // _value field
    indent( sb, indent + 2 );
    sb.append( "private final Object _value;\n" );

    // Constructor
    indent( sb, indent + 2 );
    sb.append( identifier ).append( "(Object objValue) {\n" );
    indent( sb, indent + 4 );
    sb.append( "_value = objValue;\n" );
    indent( sb, indent + 2 );
    sb.append( "}\n" );

    // toBindingValue() method
    indent( sb, indent + 2 );
    sb.append( "public Object toBindingValue() {\n" );
    indent( sb, indent + 4 );
    sb.append( "return _value;\n" );
    indent( sb, indent + 2 );
    sb.append( "}\n" );
    indent( sb, indent );

    sb.append( "}\n" );
  }
}
