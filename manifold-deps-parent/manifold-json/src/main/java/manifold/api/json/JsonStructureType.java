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

package manifold.api.json;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import javax.script.Bindings;
import manifold.api.json.schema.JsonEnumType;
import manifold.api.json.schema.JsonSchemaTransformer;
import manifold.api.json.schema.LazyRefJsonType;
import manifold.api.json.schema.TypeAttributes;
import manifold.ext.RuntimeMethods;
import manifold.ext.api.IBindingsBacked;
import manifold.json.extensions.java.net.URL.ManUrlExt;
import manifold.json.extensions.javax.script.Bindings.ManBindingsExt;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.json.schema.JsonSchemaType;
import manifold.api.json.schema.JsonUnionType;
import manifold.api.type.ActualName;
import manifold.api.type.SourcePosition;
import manifold.api.type.TypeReference;
import manifold.util.JsonUtil;
import manifold.util.ManEscapeUtil;
import manifold.util.ManStringUtil;

/**
 */
public class JsonStructureType extends JsonSchemaType
{
  private static final String FIELD_FILE_URL = "__FILE_URL_";

  private static final class State
  {
    private List<IJsonType> _superTypes;
    private Map<String, IJsonType> _membersByName;
    private Map<String, Set<IJsonType>> _unionMembers;
    private Map<String, Token> _memberLocations;
    private Map<String, IJsonParentType> _innerTypes;
    private Set<String> _required;
    private Token _token;
  }

  private final State _state;

  
  public JsonStructureType( JsonSchemaType parent, URL source, String name, TypeAttributes attr )
  {
    super( name, source, parent, attr );

    // Using State to encapsulate state which facilitates cloning
    // (this state must be shared across cloned versions, only the TypeAttributes state is separate per clone)
    _state = new State();

    // Using LinkedHashMap to preserve insertion order, an impl detail currently required by the IJ plugin for rename
    // refactoring i.e., renaming a json property should result in a source file that differs only in the naming
    // difference -- there should be no difference in ordering of methods etc.
    _state._membersByName = new LinkedHashMap<>();
    _state._memberLocations = new LinkedHashMap<>();

    _state._innerTypes = Collections.emptyMap();
    _state._unionMembers = Collections.emptyMap();
    _state._superTypes = Collections.emptyList();
    _state._required = Collections.emptySet();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();
    for( Map.Entry<String, IJsonType> entry: new LinkedHashSet<>( _state._membersByName.entrySet() ) )
    {
      IJsonType type = entry.getValue();

      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        _state._membersByName.put( entry.getKey(), ((LazyRefJsonType)type).resolve() );
      }
    }

    for( Map.Entry<String, Set<IJsonType>> entry: new LinkedHashSet<>( _state._unionMembers.entrySet() ) )
    {
      Set<IJsonType> types = new LinkedHashSet<>();
      for( IJsonType type: entry.getValue() )
      {
        if( type instanceof JsonSchemaType )
        {
          ((JsonSchemaType)type).resolveRefs();
        }
        else if( type instanceof LazyRefJsonType )
        {
          type = ((LazyRefJsonType)type).resolve();
        }
        types.add( type );
      }
      _state._unionMembers.put( entry.getKey(), types );
    }

    for( Map.Entry<String, IJsonParentType> entry: new LinkedHashSet<>( _state._innerTypes.entrySet() ) )
    {
      IJsonType type = entry.getValue();
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        type = ((LazyRefJsonType)type).resolve();
        _state._innerTypes.put( entry.getKey(), (IJsonParentType)type );
      }
    }
  }

  private boolean isSuperParentMe( IJsonType type )
  {
    return type.getParent() == this ||
           type.getParent() != null &&
           type.getParent().getName().equals( JsonSchemaTransformer.JSCH_DEFINITIONS ) &&
           type.getParent().getParent().getName().equals( getName() );
  }

  @Override
  public String getFqn()
  {
    String result = "";
    if( !isParentRoot() )
    {
      result = getParent().getFqn();
      result += '.';
    }
    return result + JsonUtil.makeIdentifier( getLabel() );
  }

  public void addSuper( IJsonType superType )
  {
    if( _state._superTypes.isEmpty() )
    {
      _state._superTypes = new ArrayList<>();
    }
    _state._superTypes.add( superType );
  }
  private List<IJsonType> getSuperTypes()
  {
    if( !_state._superTypes.isEmpty() )
    {
      if( _state._superTypes.stream().anyMatch( e -> e instanceof LazyRefJsonType ) )
      {
        List<IJsonType> resolved = new ArrayList<>();
        for( IJsonType type: _state._superTypes )
        {
          if( type instanceof LazyRefJsonType )
          {
            type = ((LazyRefJsonType)type).resolve();
          }
          resolved.add( type );
        }
        _state._superTypes = resolved;
      }
    }
    return _state._superTypes;
  }

  public void addChild( String name, IJsonParentType type )
  {
    if( _state._innerTypes.isEmpty() )
    {
      _state._innerTypes = new HashMap<>();
    }
    _state._innerTypes.put( name, type );
  }

  public IJsonType findChild( String name )
  {
    // look in inner types
    IJsonParentType innerType = _state._innerTypes.get( name );

    // look in definitions (json schema)
    if( innerType == null )
    {
      List<IJsonType> definitions = getDefinitions();
      if( definitions != null )
      {
        for( IJsonType child: definitions )
        {
          if( child.getIdentifier().equals( name ) )
          {
            innerType = (IJsonParentType)child;
            break;
          }
        }
      }
    }

    // look in union types
    if( innerType == null )
    {
      for( Set<IJsonType> constituents: _state._unionMembers.values() )
      {
        for( IJsonType c: constituents )
        {
          if( c.getIdentifier().equals( name ) )
          {
            innerType = (IJsonParentType)c;
          }
          else
          {
            while( c instanceof JsonListType )
            {
              c = ((JsonListType)c).getComponentType();
            }
            if( c.getIdentifier().equals( name ) )
            {
              innerType = (IJsonParentType)c;
            }
          }
        }
      }
    }
    return innerType;
  }

  public Map<String, IJsonType> getMembers()
  {
    return _state._membersByName;
  }
  private Map<String, IJsonType> getAllMembers()
  {
    Map<String, IJsonType> allMembers = new HashMap<>( _state._membersByName );
    for( IJsonType extended: getSuperTypes() )
    {
      if( extended instanceof JsonStructureType )
      {
        allMembers.putAll( ((JsonStructureType)extended).getMembers() );
      }
    }
    return allMembers;
  }
  protected Map<String, Token> getMemberLocations()
  {
    return _state._memberLocations;
  }

  @SuppressWarnings("unused")
  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _state._innerTypes;
  }

  public void addMember( String name, IJsonType type, Token token )
  {
    IJsonType existingType = _state._membersByName.get( name );
    if( existingType != null && existingType != type )
    {
      if( type == DynamicType.instance() )
      {
        // Keep the more specific type (the dynamic type was inferred from a 'null' value, which should not override a static type)
        return;
      }
      if( existingType != DynamicType.instance() )
      {
        type = Json.mergeTypes( existingType, type );
        if( type == null )
        {
          // if the existing type is dynamic, override it with a more specific type,
          // otherwise the types disagree...
          throw new RuntimeException( "Types disagree for '" + name + "' from array data: " + existingType.getName() + " vs: " + existingType.getName() );
        }
      }
    }
    _state._membersByName.put( name, type );
    _state._memberLocations.put( name, token );
    addUnionMemberAccess( name, type );
  }

  private void addUnionMemberAccess( String name, IJsonType type )
  {
    if( type instanceof JsonUnionType )
    {
      for( IJsonType constituent : ((JsonUnionType)type).getConstituents() )
      {
        if( _state._unionMembers.isEmpty() )
        {
          _state._unionMembers = new HashMap<>();
        }

        Set<IJsonType> union = _state._unionMembers.computeIfAbsent( name, k -> new LinkedHashSet<>() );
        union.add( constituent );
      }
    }
    else if( type instanceof JsonListType )
    {
      addUnionMemberAccess( name, ((JsonListType)type).getComponentType() );
    }
  }

  private IJsonType findMemberType( String name )
  {
    return _state._membersByName.get( name );
  }

  public Token getToken()
  {
    return _state._token;
  }
  public void setToken( Token token )
  {
    _state._token = token;
  }

  public IJsonType merge( IJsonType that )
  {
    if( !(that instanceof JsonStructureType) || that instanceof JsonEnumType )
    {
      return null;
    }

    JsonStructureType other = (JsonStructureType)that;

    if( !getName().equals( other.getName() ) )
    {
      return null;
    }

    TypeAttributes mergedTypeAttributes = getTypeAttributes().blendWith( other.getTypeAttributes() );

    JsonStructureType mergedType = new JsonStructureType( getParent(), getFile(), getName(), mergedTypeAttributes );

    for( Map.Entry<String, IJsonType> e : _state._membersByName.entrySet() )
    {
      String memberName = e.getKey();
      IJsonType memberType = other.findMemberType( memberName );
      if( memberType != null )
      {
        memberType = Json.mergeTypes( e.getValue(), memberType );
      }
      else
      {
        memberType = e.getValue();
      }

      if( memberType != null )
      {
        mergedType.addMember( memberName, memberType, _state._memberLocations.get( memberName ) );
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

  public void render( StringBuilder sb, int indent, boolean mutable )
  {
    JsonEnumType enumType = getAllOfEnumType();
    if( enumType != null )
    {
      enumType.render( sb, indent, mutable );
      return;
    }

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
    sb.append( "@Structural\n" );
    indent( sb, indent );
    sb.append( "public interface " ).append( identifier ).append( addSuperTypes( sb ) ).append( " {\n" );
    renderFileField( sb, indent + 2 );
    renderStaticMembers( sb, indent + 2 );
    renderProperties( sb, indent, mutable );
    addAdditionalPropertiesMethods( sb, indent, mutable );
    renderInnerTypes( sb, indent, mutable );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void renderInnerTypes( StringBuilder sb, int indent, boolean mutable )
  {
    addBuilder( sb, indent );
    
    for( IJsonParentType child : _state._innerTypes.values() )
    {
      child.render( sb, indent + 2, mutable );
    }
    List<IJsonType> definitions = getDefinitions();
    if( definitions != null )
    {
      for( IJsonType child : definitions )
      {
        if( child instanceof IJsonParentType )
        {
          ((IJsonParentType)child).render( sb, indent + 2, mutable );
        }
      }
    }
  }

  private void renderProperties( StringBuilder sb, int indent, boolean mutable )
  {
    renderProperties( sb, indent, mutable, new HashSet<>() );
  }
  private void renderProperties( StringBuilder sb, int indent, boolean mutable, Set<String> rendered )
  {
    for( String key : _state._membersByName.keySet() )
    {
      if( rendered.contains( key ) )
      {
        continue;
      }
      rendered.add( key );

      sb.append( '\n' );
      IJsonType type = _state._membersByName.get( key );
      String propertyType = getPropertyType( type );
      boolean isWriteOnly = type.getTypeAttributes().getWriteOnly() != null && type.getTypeAttributes().getWriteOnly();
      if( !isWriteOnly )
      {
        addGetter( sb, indent, type, key, propertyType );
      }
      boolean isReadOnly = type.getTypeAttributes().getReadOnly() != null && type.getTypeAttributes().getReadOnly();
      if( mutable && !isReadOnly )
      {
        addSetter( sb, indent, key, propertyType );
      }
      renderUnionAccessors( sb, indent, mutable, key, type );
    }

    // Include methods from super interfaces that are inner classes of this type
    // Since Java does not allow a type to extend its own inner classes.  Note such
    // a super type is not in the extends list of this interface.
    for( IJsonType superType: getSuperTypes() )
    {
      if( isSuperParentMe( superType ) && !(superType instanceof JsonEnumType) )
      {
        ((JsonStructureType)superType).renderProperties( sb, indent, mutable, rendered );
      }
    }
  }

  private void addGetter( StringBuilder sb, int indent, IJsonType type, String key, String propertyType )
  {
    addSourcePositionAnnotation( sb, indent + 2, key );
    String identifier = addActualNameAnnotation( sb, indent + 2, key, true );
    indent( sb, indent + 2 );
    sb.append( "default $propertyType get" ).append( identifier ).append( "() {\n" );
    indent( sb, indent + 4 );
    if( type instanceof JsonListType || propertyType.indexOf( '>' ) > 0 )
    {
      sb.append( "return ($propertyType)getBindings().get(\"$key\");\n" );
    }
    else
    {
      sb.append( "return ($propertyType)" ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerce(getBindings().get(\"$key\"), ${propertyType}.class);\n" );
    }
    indent( sb, indent + 2 );
    sb.append( "}\n" );
  }

  private void addSetter( StringBuilder sb, int indent, String key, String propertyType )
  {
    addSourcePositionAnnotation( sb, indent + 2, key );
    String identifier = addActualNameAnnotation( sb, indent + 2, key, true );
    indent( sb, indent + 2 );
    sb.append( "default void set" ).append( identifier ).append( "(" ).append( propertyType ).append( " ${'$'}value) {\n" );
    indent( sb, indent + 4 );
    sb.append( "getBindings().put(\"$key\", " ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerceToBindingValue(getBindings(), ${'$'}value));\n" );
    indent( sb, indent + 2 );
    sb.append( "}\n" );
  }

  private void addAdditionalPropertiesMethods( StringBuilder sb, int indent, boolean mutable )
  {
    // If additionalProperties is not defined, defaults to 'true'
    Object addProps = getTypeAttributes().getAdditionalProperties();
    boolean isAdditionalProperties = addProps == null || (!(addProps instanceof Boolean) || (boolean)addProps);
    boolean isPatternProperties = getTypeAttributes().getPatternProperties() != null && !getTypeAttributes().getPatternProperties().isEmpty();
    if( !isAdditionalProperties && !isPatternProperties )
    {
      // note if additionalProperties is false, there can still be patternProperties if they are defined, hence
      // checking both values
      return;
    }

    indent( sb, indent + 2 );
    sb.append( "default Object get(String ${'$'}name) {\n" );
    indent( sb, indent + 4 );
    sb.append( "return getBindings().get(${'$'}name);\n" );
    indent( sb, indent + 2 );
    sb.append( "}\n" );

    if( mutable )
    {
      indent( sb, indent + 2 );
      sb.append( "default Object put(String ${'$'}name, Object ${'$'}value) {\n" );
      indent( sb, indent + 4 );
      sb.append( "return getBindings().put(${'$'}name, ${'$'}value);\n" );
      indent( sb, indent + 2 );
      sb.append( "}\n" );
    }
  }

  private void renderUnionAccessors( StringBuilder sb, int indent, boolean mutable, String key, IJsonType type )
  {
    if( isCollapsedUnionEnum( type ) )
    {
      return;
    }

    String identifier;
    Set<IJsonType> union = _state._unionMembers.get( key );
    if( union != null )
    {
      for( IJsonType constituentType : union )
      {
        sb.append( '\n' );
        String specificPropertyType = getConstituentQn( constituentType, type );
        addSourcePositionAnnotation( sb, indent + 2, key );
        if( constituentType instanceof JsonSchemaType )
        {
          addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
        }
        identifier = addActualNameAnnotation( sb, indent + 2, key, true );
        indent( sb, indent + 2 );
        String unionName = makeMemberIdentifier( constituentType );
        sb.append( "default $specificPropertyType  get" ).append( identifier ).append( "As" ).append( unionName ).append( "() {\n" );
        indent( sb, indent + 4 );
        if( constituentType instanceof JsonListType || specificPropertyType.indexOf( '>' ) > 0 )
        {
          sb.append( "return ($specificPropertyType)getBindings().get(\"$key\");\n" );
        }
        else
        {
          sb.append( "return ($specificPropertyType)" ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerce(getBindings().get(\"$key\"), ${specificPropertyType}.class);\n" );
        }
        indent( sb, indent + 2 );
        sb.append( "}\n");
        if( mutable )
        {
          addSourcePositionAnnotation( sb, indent + 2, key );
          if( constituentType instanceof JsonSchemaType )
          {
            addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
          }
          addActualNameAnnotation( sb, indent + 2, key, true );
          indent( sb, indent + 2 );
          sb.append( "default void set" ).append( identifier ).append( "As" ).append( unionName ).append( "(" ).append( specificPropertyType ).append( " ${'$'}value) {\n" );
          indent( sb, indent + 4 );
          sb.append( "getBindings().put(\"$key\", " ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerceToBindingValue(getBindings(), ${'$'}value));\n" );
          indent( sb, indent + 2 );
          sb.append( "}\n" );
        }
      }
    }
  }

  private boolean isCollapsedUnionEnum( IJsonType type )
  {
    while( type instanceof JsonListType )
    {
      type = ((JsonListType)type).getComponentType();
    }
    JsonEnumType enumType = type instanceof JsonUnionType ? ((JsonUnionType)type).getCollapsedEnumType() : null;
    return enumType != null;
  }

  private JsonEnumType getAllOfEnumType()
  {
    if( !getMembers().isEmpty() )
    {
      return null;
    }

    if( getSuperTypes().stream().allMatch( e -> e instanceof JsonEnumType ) )
    {
      return makeEnumType( getSuperTypes() );
    }
    return null;
  }

  protected JsonEnumType makeEnumType( Collection<? extends IJsonType> types )
  {
    JsonEnumType result = null;
    JsonEnumType prev = null;
    for( IJsonType type: types )
    {
      result = new JsonEnumType( (JsonEnumType)type, prev, getParent(), getName() );
      prev = result;
    }
    return result;
  }

  private String getConstituentQn( IJsonType constituentType, IJsonType propertyType )
  {
    String qn = getConstituentQn( constituentType );
    while( propertyType instanceof JsonListType )
    {
      //noinspection StringConcatenationInLoop
      qn = List.class.getTypeName() + '<' + qn + '>';
      propertyType = ((JsonListType)propertyType).getComponentType();
    }
    return qn;
  }

  private String getPropertyType( IJsonType propertyType )
  {
    String name;
    if( propertyType instanceof JsonListType )
    {
      name = List.class.getTypeName() + '<' + getPropertyType( ((JsonListType)propertyType).getComponentType() ) + '>';
    }
    else if( propertyType instanceof JsonUnionType )
    {
      JsonEnumType enumType = ((JsonUnionType)propertyType).getCollapsedEnumType();
      name = enumType != null
             ? getNameRelativeFromMe( enumType )
             : Object.class.getSimpleName();
    }
    else
    {
      name = propertyType instanceof JsonSchemaType
             ? getNameRelativeFromMe( propertyType )
             : propertyType.getIdentifier();
    }
    return name;
  }

  private String getNameRelativeFromMe( IJsonType type )
  {
    IJsonType parent = getParentFromMe( type );
    if( parent == null )
    {
      return type.getIdentifier();
    }

    return getNameRelativeFromMe( parent ) + '.' + type.getIdentifier();
  }
  private IJsonType getParentFromMe( IJsonType type )
  {
    IJsonParentType parent = type.getParent();
    if( parent != null )
    {
      if( parent instanceof JsonListType )
      {
        return getParentFromMe( parent );
      }

      if( parent.getIdentifier().equals( JsonSchemaTransformer.JSCH_DEFINITIONS ) )
      {
        return getParentFromMe( parent );
      }

      if( parent == this )
      {
        return null;
      }
    }

    return parent;
  }

  private String getConstituentQn( IJsonType constituentType )
  {
    if( constituentType instanceof JsonListType )
    {
      return List.class.getTypeName() + '<' + getConstituentQn( ((JsonListType)constituentType).getComponentType() ) + '>';
    }

    String qn = "";
    if( constituentType.getParent() instanceof JsonUnionType )
    {
      qn = getConstituentQn( constituentType.getParent() ) + '.';
    }
    return qn + constituentType.getIdentifier();
  }
  private IJsonType getConstituentQnComponent( IJsonType constituentType )
  {
    if( constituentType instanceof JsonListType )
    {
      return getConstituentQnComponent( ((JsonListType)constituentType).getComponentType() );
    }
    return constituentType;
  }

  private void renderFileField( StringBuilder sb, int indent )
  {
    renderFileField( sb, indent, null );
  }
  protected void renderFileField( StringBuilder sb, int indent, String modifiers )
  {
    indent( sb, indent );
    sb.append( modifiers == null ? "" : modifiers + " " ).append( "String " ).append( FIELD_FILE_URL ).append( " = \"" ).append( getFile() == null ? "null" : getFile().toString() ).append( "\";\n" );
  }

  private String addSuperTypes( StringBuilder sb )
  {
    sb.append( " extends " ).append( IBindingsBacked.class.getSimpleName() );

    List<IJsonType> superTypes = getSuperTypes();
    if( superTypes.isEmpty() )
    {
      return "";
    }
    
    for( IJsonType superType: superTypes )
    {
      // Java does not allow extending your own inner class,
      // instead we will grab all the methods from this later.
      // See renderProperties().
      if( !isSuperParentMe( superType ) )
      {
        sb.append( ", " );
        sb.append( superType.getIdentifier() );
      }
    }
    return "";
  }

  protected String addActualNameAnnotation( StringBuilder sb, int indent, String name, boolean capitalize )
  {
    String identifier = makeIdentifier( name, capitalize );
    if( !identifier.equals( name ) )
    {
      indent( sb, indent );
      sb.append( "@" ).append( ActualName.class.getName() ).append( "( \"" ).append( name ).append( "\" )\n" );
    }
    return identifier;
  }

  private String makeMemberIdentifier( IJsonType type )
  {
    return makeIdentifier( type.getName(), false );
  }
  private String makeIdentifier( String name, boolean capitalize )
  {
    return capitalize ? ManStringUtil.capitalize( JsonUtil.makeIdentifier( name ) ) : JsonUtil.makeIdentifier( name );
  }

  public boolean addSourcePositionAnnotation( StringBuilder sb, int indent, String name )
  {
    Token token = _state._memberLocations.get( name );
    if( token == null )
    {
      return false;
    }
    return addSourcePositionAnnotation( sb, indent, name, token );
  }
  protected boolean addSourcePositionAnnotation( StringBuilder sb, int indent, String name, Token token )
  {
    SrcAnnotationExpression annotation = new SrcAnnotationExpression( SourcePosition.class.getName() )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( getIdentifier(), FIELD_FILE_URL ) ).name( "url" ) )
      .addArgument( "feature", String.class, name )
      .addArgument( "offset", int.class, token.getOffset() )
      .addArgument( "length", int.class, name.length() );
    annotation.render( sb, indent );
    return true;
  }

  private void addTypeReferenceAnnotation( StringBuilder sb, int indent, JsonSchemaType type )
  {
    SrcAnnotationExpression annotation = new SrcAnnotationExpression( TypeReference.class.getName() )
      .addArgument( "value", String.class, getPropertyType( type ) );
    annotation.render( sb, indent );
  }

  private void renderStaticMembers( StringBuilder sb, int indent )
  {
    String typeName = getIdentifier();

    // Add a static create(...) method having parameters corresponding with "required" properties not having
    // a "default" value.
    addCreateMethod( sb, indent, typeName );

    // Provide a builder(...) method having parameters matching create(...) above and providing
    // withXxx( x ) methods corresponding with non "required" properties
    addBuilderMethod( sb, indent );

    // Called reflectively from RuntimeMethods, this proxy and the default get/set method impls defined here enable the
    // JSON type manifold to avoid the overhead of dynamic proxy generation and compilation at runtime. Otherwise the
    // ICallHandler-based dynamic proxy would be used, which causes a significant delay the first time a JSON interface
    // is used.
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " proxy(" ).append( Bindings.class.getSimpleName() ).append( " bindings) {\n" );
    indent( sb, indent + 2 );
    sb.append( "return new $typeName() {\n" );
    indent( sb, indent + 4 );
    sb.append( "  public Bindings getBindings() {\n" );
    indent( sb, indent + 6 );
    sb.append( "    return bindings;\n" );
    indent( sb, indent + 4 );
    sb.append( "  }\n" );
    indent( sb, indent + 2 );
    sb.append( "};\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    if( shouldRenderTopLevel( this ) )
    {
      // Only add factory methods to top-level json structure
      addTopLevelFactoryMethods( sb, indent, typeName );
    }

    // These are all implemented by Bindings via ManBindingsExt
    indent( sb, indent );
    sb.append( "default String" ).append( " toJson() {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toJson(getBindings());\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    sb.append( "default String" ).append( " toXml() {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toXml(getBindings());\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    sb.append( "default String" ).append( " toXml(String name) {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toXml(getBindings(), name);\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addTopLevelFactoryMethods( StringBuilder sb, int indent, String typeName )
  {
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " fromJson(String jsonText) {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( typeName ).append( ")" ).append( Json.class.getName() ).append( ".fromJson(jsonText);\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " fromJsonUrl(String url) {\n" );
    indent( sb, indent );
    sb.append( "  try {\n" );
    indent( sb, indent );
    sb.append( "    return (" ).append( typeName ).append( ")" ).append( ManUrlExt.class.getName() ).append( ".getJsonContent(new java.net.URL(url));\n" );
    indent( sb, indent );
    sb.append( "  } catch(Exception e) {throw new RuntimeException(e);}\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " fromJsonUrl(java.net.URL url) {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( typeName ).append( ")" ).append( ManUrlExt.class.getName() ).append( ".getJsonContent(url);\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " fromJsonUrl(java.net.URL url, javax.script.Bindings json) {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( typeName ).append( ")" ).append( ManUrlExt.class.getName() ).append( ".postForJsonContent(url, json);\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " fromJsonFile(java.io.File file) {\n" );
    indent( sb, indent );
    sb.append( "  try {\n" );
    indent( sb, indent );
    sb.append( "    return (" ).append( typeName ).append( ")fromJsonUrl(file.toURI().toURL());\n" );
    indent( sb, indent );
    sb.append( "  } catch(Exception e) {throw new RuntimeException(e);}\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addBuilderMethod( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( "static Builder builder(" );
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    addRequredParams( sb, allRequired, allMembers );
    sb.append( ") {\n" );
    indent += 2;
    indent( sb, indent );
    sb.append( "return new Builder(" );
    int count = 0;
    for( String param: allRequired )
    {
      IJsonType paramType = allMembers.get( param );
      if( paramType.getTypeAttributes().getDefaultValue() == null )
      {
        if( count++ > 0 )
        {
          sb.append( ", " );
        }
        //noinspection unused
        String paramName = makeIdentifier( param, false );
        sb.append( "$paramName" );
      }
    }
    sb.append( ");\n" );
    indent -= 2;
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addBuilder( StringBuilder sb, int indent )
  {
    indent( sb, indent += 2 );
    sb.append( "class Builder {\n" );
    indent( sb, indent += 2 );
    sb.append( "private final Bindings _bindings;\n" );
    indent( sb, indent );

    // constructor
    addBuilderConstructor( sb, indent );

    addWithMethods( sb, indent );

    addBuildMethod( sb, indent );

    indent( sb, indent - 2 );
    sb.append( "}\n" );
  }

  private void addBuildMethod( StringBuilder sb, int indent )
  {
    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "public $typeName build() {\n" );
    indent( sb, indent+2 );
    sb.append( "return ($typeName)_bindings;\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addWithMethods( StringBuilder sb, int indent )
  {
    for( Map.Entry<String, IJsonType> entry: getNotRequired().entrySet() )
    {
      //noinspection unused
      String propertyType = getPropertyType( entry.getValue() );
      String key = entry.getKey();
      //noinspection unused
      String suffix = makeIdentifier( key, true );
      addSourcePositionAnnotation( sb, indent + 2, key );
      //noinspection unused
      String identifier = addActualNameAnnotation( sb, indent + 2, key, false );
      indent( sb, indent );
      sb.append( "public Builder with$suffix($propertyType $identifier) {\n" );
      indent( sb, indent+2 );
      sb.append( "_bindings.put(\"$key\", $identifier);\n" );
      indent( sb, indent+2 );
      sb.append( "return this;\n" );
      indent( sb, indent );
      sb.append( "}\n" );
    }
  }

//  private Map<String, IJsonType> getAllReadOnly()
//  {
//    Map<String, IJsonType> readOnlyProps = new HashMap<>();
//    for( Map.Entry<String, IJsonType> entry: getAllMembers().entrySet() )
//    {
//      Boolean readOnly = entry.getValue().getTypeAttributes().getReadOnly();
//      if( readOnly != null && readOnly )
//      {
//        readOnlyProps.put( entry.getKey(), entry.getValue() );
//      }
//    }
//    return readOnlyProps;
//  }
  private Map<String, IJsonType> getNotRequired()
  {
    Map<String, IJsonType> result = new HashMap<>();
    Set<String> allRequired = getAllRequired();
    getAllMembers().forEach( (key, value) -> {
      if( !allRequired.contains( key ) )
      {
        result.put( key, value );
      }
    } );
    return result;
  }


  private void addBuilderConstructor( StringBuilder sb, int indent )
  {
    sb.append( "private Builder(" );
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    addRequredParams( sb, allRequired, allMembers );
    sb.append( ") {\n" );
    indent += 2;
    indent( sb, indent );
    sb.append( "_bindings = create(" );
    int count = 0;
    for( String param: allRequired )
    {
      IJsonType paramType = allMembers.get( param );
      if( paramType.getTypeAttributes().getDefaultValue() == null )
      {
        if( count++ > 0 )
        {
          sb.append( ", " );
        }
        //noinspection unused
        String paramName = makeIdentifier( param, false );
        sb.append( "$paramName" );
      }
    }
    sb.append( ").getBindings();\n" );
    indent -= 2;
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addCreateMethod( StringBuilder sb, int indent, String typeName )
  {
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " create(" );
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    addRequredParams( sb, allRequired, allMembers );
    sb.append( ") {\n" );
    indent( sb, indent + 2 );
    sb.append( "SimpleBindings bindings_ = new SimpleBindings();\n" );
    for( String requiredProp: allRequired )
    {
      IJsonType paramType = allMembers.get( requiredProp );
      if( paramType.getTypeAttributes().getDefaultValue() == null )
      {
        indent( sb, indent + 2 );
        //noinspection unused
        String passedInParam = makeIdentifier( requiredProp, false );
        sb.append( "bindings_.put(\"$requiredProp\", $passedInParam);\n" );
      }
    }
    for( Map.Entry<String, IJsonType> entry: allMembers.entrySet() )
    {
      Object defaultValue = entry.getValue().getTypeAttributes().getDefaultValue();
      if( defaultValue != null )
      {
        indent( sb, indent + 2 );
        sb.append( "bindings_.put(\"${entry.getKey()}\", " );
        if( defaultValue instanceof Bindings )
        {
          indent( sb, indent + 2 );
          sb.append( "Json.fromJson(\"" );
          StringBuilder defValJson = new StringBuilder();
          JsonUtil.toJson( defValJson, 0, defaultValue );
          sb.append( ManEscapeUtil.escapeForJava( defValJson.toString() ) );
          sb.append( "\")" );
        }
        else if( defaultValue instanceof List )
        {
          indent( sb, indent + 2 );
          sb.append( "Json.fromJson(\"" );
          StringBuilder defValJson = new StringBuilder();
          JsonUtil.toJson( defValJson, 0, defaultValue );
          sb.append( ManEscapeUtil.escapeForJava( defValJson.toString() ) );
          sb.append( "\").get(\"value\")" );
        }
        else
        {
          JsonUtil.toJson( sb, 0, defaultValue );
        }
        sb.append( ");\n" );
      }
    }
    indent( sb, indent + 2 );
    sb.append( "return ($typeName)bindings_;\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addRequredParams( StringBuilder sb, Set<String> allRequired, Map<String, IJsonType> allMembers )
  {
    int count = 0;
    for( String param: allRequired )
    {
      IJsonType paramType = allMembers.get( param );
      if( paramType.getTypeAttributes().getDefaultValue() == null )
      {
        if( count++ > 0 )
        {
          sb.append( ", " );
        }
        //noinspection unused
        String paramTypeName = getPropertyType( paramType );
        //noinspection unused
        String paramName = makeIdentifier( param, false );
        sb.append( "$paramTypeName $paramName" );
      }
    }
  }

  private boolean shouldRenderTopLevel( IJsonParentType type )
  {
    IJsonParentType parent = type.getParent();
    if( parent == null )
    {
      return true;
    }

    if( parent instanceof JsonListType )
    {
      return shouldRenderTopLevel( parent );
    }

    return false;
  }

  protected void indent( StringBuilder sb, int indent )
  {
    for( int i = 0; i < indent; i++ )
    {
      sb.append( ' ' );
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
    if( !super.equals( o ) )
    {
      return false;
    }

    JsonStructureType that = (JsonStructureType)o;

    int[] i = {0};
    if( _state._superTypes.size() != that._state._superTypes.size() ||
        !_state._superTypes.stream().allMatch( t -> that._state._superTypes.get( i[0]++ ).equalsStructurally( t ) ) )
    {
      return false;
    }
    if( !(_state._membersByName.size() == that._state._membersByName.size() &&
          _state._membersByName.keySet().stream().allMatch(
            key -> that._state._membersByName.containsKey( key ) &&
                   _state._membersByName.get( key ).equalsStructurally( that._state._membersByName.get( key ) ) )) )
    {
      return false;
    }
    if( !(_state._unionMembers.size() == that._state._unionMembers.size() &&
          _state._unionMembers.keySet().stream().allMatch(
            key -> that._state._unionMembers.containsKey( key ) &&
                   typeSetsSame( _state._unionMembers.get( key ), that._state._unionMembers.get( key ) ) )) )
    {
      return false;
    }
    return _state._innerTypes.size() == that._state._innerTypes.size() &&
           _state._innerTypes.keySet().stream().allMatch(
             key -> that._state._innerTypes.containsKey( key ) &&
                    _state._innerTypes.get( key ).equalsStructurally( that._state._innerTypes.get( key ) ) );
  }

  private boolean typeSetsSame( Set<IJsonType> t1, Set<IJsonType> t2 )
  {
    if( t1.size() != t2.size() )
    {
      return false;
    }
    Iterator<IJsonType> iter1 = t1.iterator();
    Iterator<IJsonType> iter2 = t2.iterator();
    while( iter1.hasNext() && iter2.hasNext() )
    {
      if( !iter1.next().equalsStructurally( iter2.next() ) )
      {
        return false;
      }
    }
    return !iter1.hasNext() && !iter2.hasNext();
  }

  @SuppressWarnings({"WeakerAccess", "unused"})
  public boolean isRequired( String name )
  {
    if( _state._required.contains( name ) )
    {
      return true;
    }
    for( IJsonType extended: getSuperTypes() )
    {
      if( extended instanceof JsonStructureType && ((JsonStructureType)extended).isRequired( name ) )
      {
        return true;
      }
    }
    return false;
  }
  private Set<String> getAllRequired()
  {
    Set<String> allRequired = new LinkedHashSet<>();
    for( IJsonType extended: getSuperTypes() )
    {
      if( extended instanceof JsonStructureType )
      {
        allRequired.addAll( ((JsonStructureType)extended)._state._required );
      }
    }
    allRequired.addAll( _state._required  );
    return allRequired;
  }
  public void addRequired( Set<String> required )
  {
    if( _state._required.isEmpty() )
    {
      _state._required = new LinkedHashSet<>();
    }
    _state._required.addAll( required );
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
      //noinspection ConstantConditions
      return this == o;
    }

    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    if( !super.equals( o ) )
    {
      return false;
    }

    JsonStructureType type = (JsonStructureType)o;

    if( !_state._superTypes.equals( type._state._superTypes ) )
    {
      return false;
    }
    if( !_state._membersByName.equals( type._state._membersByName ) )
    {
      return false;
    }
    if( !_state._unionMembers.equals( type._state._unionMembers ) )
    {
      return false;
    }
    return _state._innerTypes.equals( type._state._innerTypes );
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _state._superTypes.hashCode();
    result = 31 * result + _state._membersByName.keySet().hashCode();
    result = 31 * result + _state._unionMembers.keySet().hashCode();
    return result;
  }

  public String toString()
  {
    return getFqn();
  }
}
