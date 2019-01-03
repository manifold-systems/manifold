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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manifold.api.gen.SrcElement;
import manifold.api.gen.SrcType;
import manifold.api.json.IJsonType;
import manifold.api.json.JsonSimpleType;
import manifold.api.json.JsonSimpleTypeWithDefault;
import manifold.api.json.JsonStructureType;
import manifold.api.json.Token;
import manifold.ext.api.IBindingType;
import manifold.util.DebugLogUtil;
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

  JsonEnumType( JsonSchemaType parent, URL source, String name, List<?> list )
  {
    super( parent, source, name );

    _enumValues = new ArrayList<>();
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

      IJsonType type = JsonSimpleType.get( value );
      addMember( JsonUtil.makeIdentifier( String.valueOf( value ) ), type, token );
      _enumValues.add( value );
    }
  }

  private JsonEnumType( JsonEnumType enum1, JsonEnumType enum2 )
  {
    super( enum1.getParent(), enum1.getFile(), enum1.getName() );

    Map<String, IJsonType> members = new HashMap<>( enum1.getMembers() );
    members.putAll( enum2.getMembers() );
    Map<String, Token> memberLocations = new HashMap<>( enum1.getMemberLocations() );
    memberLocations.putAll( enum2.getMemberLocations() );
    members.forEach( (m, v) -> addMember( m, v, memberLocations.get( m ) ) );

    Set<Object> enumValues = new HashSet<>( enum1._enumValues );
    enumValues.addAll( enum2._enumValues );
    _enumValues = new ArrayList<>( enumValues );
  }

  @Override
  public IJsonType merge( IJsonType that )
  {
    if( !getName().equals( that.getName() ) )
    {
      return null;
    }

    if( that instanceof JsonSimpleType || that instanceof JsonSimpleTypeWithDefault )
    {
      return that;
    }
    
    if( !(that instanceof JsonEnumType) )
    {
      return null;
    }

    return new JsonEnumType( this, (JsonEnumType)that );
  }

  @Override
  public void render( StringBuilder sb, int indent, boolean mutable )
  {
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

    DebugLogUtil.log( "c:\\temp\\enumlog.log", sb.toString(), true );
  }
}
