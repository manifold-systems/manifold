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

package manifold.api.json.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import javax.script.Bindings;
import manifold.ext.DataBindings;
import manifold.api.util.Pair;


import static manifold.api.json.schema.JsonSchemaTransformer.*;
import static manifold.api.json.schema.TypeAttributes.AttributeTypes.OVERRIDE;

/**
 * TypeAttributes contains attributes that "decorate" a type.  These attributes modify an existing type
 * with information separate from the type's internal definition.  Such attributes include "nullable",
 * "default value", "readOnly", etc.
 */
public final class TypeAttributes
{
  private Map<String, Object> _attributes;

  public TypeAttributes()
  {
    this( null, new DataBindings() );
  }

  public TypeAttributes( Boolean nullable )
  {
    this( nullable, new DataBindings() );
  }

  public TypeAttributes( Bindings bindings )
  {
    this( null, new DataBindings() );
    assignAttribute( bindings, JSCH_NULLABLE );
  }

  public TypeAttributes( Boolean nullable, Bindings jsonObj )
  {
    _attributes = new HashMap<>();
    _attributes.put( JSCH_NULLABLE, nullable );
    assignAttributes( jsonObj );
  }

  private TypeAttributes( Map<String, Object> attributes )
  {
    _attributes = attributes;
  }

  private void assignAttributes( Bindings jsonObj )
  {
    for( String attr: AttributeTypes.INSTANCE.getNames() )
    {
      if( attr.equals( JSCH_NULLABLE ) )
      {
        continue;
      }

      assignAttribute( jsonObj, attr );
    }
  }

  private void assignAttribute( Bindings jsonObj, String attr )
  {
    boolean hasAttr = jsonObj.containsKey( attr );
    if( hasAttr )
    {
      Object attrValue = jsonObj.get( attr );
      if( attrValue instanceof Pair )
      {
        attrValue = ((Pair)attrValue).getSecond();
      }
      _attributes.put( attr, attrValue );
    }
  }

  public TypeAttributes overrideWith( TypeAttributes other )
  {
    HashMap<String, Object> attributes = new HashMap<>( _attributes );
    for( Map.Entry<String, Object> otherAttr: other._attributes.entrySet() )
    {
      Object existingValue = _attributes.get( otherAttr.getKey() );
      Object otherValue = otherAttr.getValue();
      attributes.put( otherAttr.getKey(), OVERRIDE.apply( existingValue, otherValue ) );
    }
    return new TypeAttributes( attributes );
  }

  public TypeAttributes blendWith( TypeAttributes other )
  {
    HashMap<String, Object> attributes = new HashMap<>();
    for( Map.Entry<String, Object> myAttr: _attributes.entrySet() )
    {
      Object myValue = myAttr.getValue();
      String name = myAttr.getKey();
      Object otherValue = other._attributes.get( name );
      //noinspection unchecked
      attributes.put( name, AttributeTypes.INSTANCE.getMerger( name ).apply( myValue, otherValue ) );
    }
    return new TypeAttributes( attributes );
  }

  public TypeAttributes copy()
  {
    HashMap<String, Object> copy = new HashMap<>( _attributes );
    return new TypeAttributes( copy );
  }

  public Boolean getNullable()
  {
    return (Boolean)_attributes.get( JSCH_NULLABLE );
  }
  void setNullable( Boolean nullable )
  {
    _attributes.put( JSCH_NULLABLE, nullable );
  }

  public Object getDefaultValue()
  {
    return _attributes.get( JSCH_DEFAULT );
  }
  public void setDefaultValue( Object value )
  {
    _attributes.put( JSCH_DEFAULT, value );
  }

  public Boolean getReadOnly()
  {
    return (Boolean)_attributes.get( JSCH_READONLY );
  }

  public Boolean getWriteOnly()
  {
    return (Boolean)_attributes.get( JSCH_WRITEONLY );
  }

  public Object getAdditionalProperties()
  {
    return _attributes.get( JSCH_ADDITIONNAL_PROPERTIES );
  }

  public Bindings getPatternProperties()
  {
    return (Bindings)_attributes.get( JSCH_PATTERN_PROPERTIES );
  }

  public static Boolean or( Boolean b1, Boolean b2 )
  {
    Boolean nullableValue = b1;
    if( nullableValue == null )
    {
      nullableValue = b2;
    }
    else if( b2 != null )
    {
      nullableValue |= b2;
    }
    return nullableValue;
  }

  public static Boolean and( Boolean b1, Boolean b2 )
  {
    Boolean nullableValue = b1;
    if( nullableValue == null )
    {
      nullableValue = b2;
    }
    else if( b2 != null )
    {
      nullableValue &= b2;
    }
    return nullableValue;
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
    TypeAttributes that = (TypeAttributes)o;
    return Objects.equals( _attributes, that._attributes );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _attributes );
  }

  static class AttributeTypes
  {
    // Given o1 and o2, if o1 and o2 are not null, o1 && o2, otherwise the non-null value
    private static final BiFunction<Boolean, Boolean, Boolean> TRUE_IF_BOTH_TRUE = AttributeTypes::trueIfBothTrue;
    // Given o1 and o2, if o1 and o2 are not null, o1 || o2, otherwise the non-null value
    @SuppressWarnings("unused")
    private static final BiFunction<Boolean, Boolean, Boolean> TRUE_IF_ONE_TRUE = AttributeTypes::trueIfOneTrue;
    static final BiFunction<Object, Object, Object> OVERRIDE = (o1, o2) -> (o2 != null) ? o2 : o1;
    static final BiFunction<Bindings, Bindings, Bindings> MERGE = (o1, o2) -> {
      DataBindings o3 = new DataBindings();
      if( o1 != null ) o3.putAll( o1 );
      if( o2 != null ) o3.putAll( o2 );
      return o3;
    };

    private static final AttributeTypes INSTANCE = new AttributeTypes();

    private final Map<String, BiFunction> _types;

    private AttributeTypes()
    {
      _types = new HashMap<>();
      _types.put( JSCH_NULLABLE, TRUE_IF_BOTH_TRUE );
      _types.put( JSCH_READONLY, TRUE_IF_BOTH_TRUE );
      _types.put( JSCH_WRITEONLY, TRUE_IF_BOTH_TRUE );
      _types.put( JSCH_DEFAULT, OVERRIDE );
      _types.put( JSCH_ADDITIONNAL_PROPERTIES, OVERRIDE );
      _types.put( JSCH_PATTERN_PROPERTIES, MERGE );
    }

    private static Boolean trueIfOneTrue( Boolean o1, Boolean o2 )
    {
      if( o1 != null && o2 != null )
        return o1 || o2;
      if( o1 != null )
         return o1;
      return o2;
    }

    private static Boolean trueIfBothTrue( Boolean o1, Boolean o2 )
    {
      if( o1 != null && o2 != null )
        return o1 && o2;
      if( o1 != null )
        return o1;
      return o2;
    }

    Set<String> getNames()
    {
      return _types.keySet();
    }

    BiFunction getMerger( String name )
    {
      return _types.get( name );
    }
  }
}
