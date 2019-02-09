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
import java.util.stream.Collectors;
import javax.script.Bindings;
import manifold.api.json.schema.JsonEnumType;
import manifold.api.json.schema.JsonSchemaTransformer;
import manifold.api.json.schema.LazyRefJsonType;
import manifold.api.json.schema.TypeAttributes;
import manifold.ext.DataBindings;
import manifold.ext.RuntimeMethods;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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
import manifold.internal.javac.IIssue;
import manifold.util.JsonUtil;
import manifold.util.ManEscapeUtil;
import manifold.util.ManStringUtil;
import manifold.util.Pair;

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
    private Set<Object> _requiredWithTokens;
    private Token _token;
  }

  private final State _state;

  //
  // State used exclusively during code generation, after resolve
  //
  private Map<String, IJsonType> _allMembers;
  private Set<String> _allRequired;

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
    _state._requiredWithTokens = Collections.emptySet();
    _state._required = Collections.emptySet();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();

    resolveSuperTypes();
    resolveMembers();
    resolveUnions();
    resolveInnerTypes();
    resolveAndVerifyRequiredProperties();
  }

  private void resolveSuperTypes()
  {
    ArrayList<IJsonType> copy = new ArrayList<>( _state._superTypes );
    for( int i = 0; i < copy.size(); i++ )
    {
      IJsonType type = copy.get( i );
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        type = ((LazyRefJsonType)type).resolve();
        _state._superTypes.set( i, type );
      }
    }
  }

  private void resolveMembers()
  {
    for( Map.Entry<String, IJsonType> entry: new LinkedHashSet<>( _state._membersByName.entrySet() ) )
    {
      IJsonType type = entry.getValue();

      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        type = ((LazyRefJsonType)type).resolve();
        _state._membersByName.put( entry.getKey(), type );
        addUnionMemberAccess( entry.getKey(), type );
      }

//      if( type instanceof JsonStructureType )
//      {
//        resolveInvertedUnionMember( entry.getKey(), (JsonStructureType)type );
//      }
    }
  }

  /**
   * If any of the super types is a union, this structure type must be converted to a union where each constituent of
   * the super type union[s] merges with the non-union super types and base type of this structure type.
   * ## todo:
   */
  private void resolveInvertedUnionMember( String name, JsonStructureType type )
  {
    Set<? extends IJsonType> unionConstituents = type.getSuperTypes().stream()
      .filter( e -> e instanceof JsonUnionType )
      .flatMap( e -> ((JsonUnionType)e).getConstituents().stream() )
      .collect( Collectors.toSet() );
    if( unionConstituents.isEmpty() )
    {
      return;
    }
    JsonUnionType unionType = new JsonUnionType( type.getParent(), type.getFile(), type.getName(), type.getTypeAttributes().copy() );
    unionType.setJsonSchema();
    type.getParent().addChild( unionType.getLabel(), unionType );
    type.getInnerTypes().forEach( ( n, inner ) -> {
      if( !(inner instanceof JsonUnionType) )
      {
        unionType.addChild( n, inner );
        if( inner instanceof JsonSchemaType )
        {
          ((JsonSchemaType)inner).setParent( unionType );
        }
      }
    } );
    for( IJsonType constituent: unionConstituents )
    {
      JsonStructureType newConstituent = new JsonStructureType( unionType, type.getFile(), constituent.getName(), constituent.getTypeAttributes().copy() );
      newConstituent.addSuper( constituent );
      type.getSuperTypes().forEach( newConstituent::addSuper ); // union supers are not included for code gen
      type.getMembers().forEach( (n, member) -> newConstituent.addMember( n, member, type.getMemberLocations().get( n ) ) );
      unionType.addConstituent( constituent.getName(), newConstituent );
    }
    _state._membersByName.remove( name );
    Token token = _state._memberLocations.remove( name );
    addMember( name, unionType, token );
  }

  private void resolveUnions()
  {
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
  }

  private void resolveInnerTypes()
  {
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

  private void resolveAndVerifyRequiredProperties()
  {
    // assign required names, removing names that do not correspond to a property, and marking such names with an error
    if( _state._requiredWithTokens != null )
    {
      for( Object req: _state._requiredWithTokens )
      {
        Object requiredNames;
        if( req instanceof Pair )
        {
          // verify each name corresponds with a property

          Token token = ((Token[])((Pair)req).getFirst())[0];
          requiredNames = ((Pair)req).getSecond();
          if( requiredNames instanceof Collection )
          {
            //noinspection unchecked
            verifyAllRequired( (Collection<String>)requiredNames, token );
          }
        }
        else
        {
          requiredNames = req;
        }

        if( requiredNames instanceof Collection )
        {
          //noinspection unchecked
          addRequired( (Collection<String>)requiredNames );
        }
      }
      _state._requiredWithTokens = Collections.emptySet();
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
    return _state._superTypes;
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
    if( _allMembers != null )
    {
      return _allMembers;
    }

    Map<String, IJsonType> allMembers = new LinkedHashMap<>( _state._membersByName );
    for( IJsonType extended: getSuperTypes() )
    {
      if( extended instanceof JsonStructureType )
      {
        allMembers.putAll( ((JsonStructureType)extended).getMembers() );
      }
    }
    return _allMembers = allMembers;
  }
  protected Map<String, Token> getMemberLocations()
  {
    return _state._memberLocations;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
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
          _state._unionMembers = new LinkedHashMap<>();
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
    sb.append( "public interface " ).append( identifier ).append( addSuperTypes( sb, identifier ) ).append( " {\n" );
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
    addProxy( sb, indent );
    addBuilder( sb, indent );
    addCopier( sb, indent );

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
    //noinspection unused
    String identifier = addActualNameAnnotation( sb, indent + 2, key, true );
    indent( sb, indent + 2 );
    sb.append( "default $propertyType get$identifier() {\n" );
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
    //noinspection unused
    String identifier = addActualNameAnnotation( sb, indent + 2, key, true );
    indent( sb, indent + 2 );
    sb.append( "default void set$identifier(" ).append( propertyType ).append( " ${'$'}value) {\n" );
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

  private String addSuperTypes( StringBuilder sb, @SuppressWarnings("unused") String ifaceName )
  {
    //noinspection unused
    String IJsonBindingsBacked = IJsonBindingsBacked.class.getSimpleName();
    sb.append( " extends $IJsonBindingsBacked" );

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
      if( !isSuperParentMe( superType ) &&
          superType instanceof JsonStructureType &&
          !(superType instanceof JsonUnionType) )
      {
        sb.append( ", " );
        sb.append( getPropertyType( superType ) );
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

    // Similar to builder(), copier() takes an instance to copy and has withXxx() methods, and a copy() method
    addCopierMethod( sb, indent );

    // Add a simple copy method for a deep copy
    addCopyMethod( sb, indent );

    // Provide a loader(...) method, returns Loader<typeName> with methods for loading content from String, URL, file, etc.
    addLoadMethod( sb, indent, typeName );

    // Called reflectively from RuntimeMethods, this proxy and the default get/set method impls defined here enable the
    // JSON type manifold to avoid the overhead of dynamic proxy generation and compilation at runtime. Otherwise the
    // ICallHandler-based dynamic proxy would be used, which causes a significant delay the first time a JSON interface
    // is used.
    indent( sb, indent );
    //noinspection unused
    String Bindings = Bindings.class.getSimpleName();
    sb.append( "static " ).append( typeName ).append( " proxy(final $Bindings bindings) {\n" );
    indent( sb, indent );
    sb.append( "  return new Proxy(bindings);\n" );
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private void addBuilderMethod( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( "static Builder builder(" );
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    addRequiredParams( sb, allRequired, allMembers );
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

  private void addCopierMethod( StringBuilder sb, int indent )
  {
    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "static Copier copier($typeName from) {return new Copier(from);}\n" );
  }

  /**
   * Not so much a "proxy" as a substitute for a structural proxy that is otherwise generated dynamically at runtime.
   * Essentially this class is a compile-time substitute that vastly improves the first-time load performance of
   * JSON types.
   */
  private void addProxy( StringBuilder sb, int indent )
  {
    //noinspection unused
    String typeName = getIdentifier();
    //noinspection unused
    String Bindings = Bindings.class.getSimpleName();
    indent( sb, indent += 2 );
    sb.append( "class Proxy implements $typeName {\n" );
    indent( sb, indent );
    sb.append( "  private final $Bindings _bindings;\n" );
    indent( sb, indent );
    sb.append( "  private Proxy($Bindings bindings) {_bindings = bindings;}\n" );
    indent( sb, indent );
    sb.append( "  public $Bindings getBindings() {return _bindings;}\n" );
    indent( sb, indent + 2 );
    sb.append( "}\n" );
  }

  private void addBuilder( StringBuilder sb, int indent )
  {
    indent( sb, indent += 2 );
    sb.append( "class Builder {\n" );
    indent( sb, indent += 2 );
    //noinspection unused
    String Bindings = Bindings.class.getSimpleName();
    sb.append( "private final $Bindings _bindings;\n" );
    indent( sb, indent );

    // constructor
    addBuilderConstructor( sb, indent );

    addWithMethods( getNotRequired(), "Builder", sb, indent );

    addBuildMethod( sb, indent );

    indent( sb, indent - 2 );
    sb.append( "}\n" );
  }

  private void addBuilderConstructor( StringBuilder sb, int indent )
  {
    sb.append( "private Builder(" );
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    addRequiredParams( sb, allRequired, allMembers );
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

  private void addCopier( StringBuilder sb, int indent )
  {
    //noinspection unused
    String Bindings = Bindings.class.getSimpleName();
    indent( sb, indent );
    sb.append( "class Copier {\n" );
    indent( sb, indent );
    sb.append( "  private final $Bindings _bindings;\n" );
    indent( sb, indent );

    // constructor
    addCopierConstructor( sb, indent );

    addWithMethods( getNotRequired(), "Copier", sb, indent );

    addCopierCopyMethod( sb, indent );

    indent( sb, indent - 2 );
    sb.append( "}\n" );
  }

  private void addCopierCopyMethod( StringBuilder sb, int indent )
  {
    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "public $typeName copy() {return ($typeName)_bindings;}\n" );
  }

  private void addCopierConstructor( StringBuilder sb, int indent )
  {
    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "private Copier($typeName from) {_bindings = from.copy().getBindings();}\n" );
  }

  private void addCopyMethod( StringBuilder sb, int indent )
  {
    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "default $typeName copy() {return ($typeName)getBindings().deepCopy();}\n" );
  }

  private void addWithMethods( Map<String, IJsonType> fields, @SuppressWarnings("unused") String builderType, StringBuilder sb, int indent )
  {
    for( Map.Entry<String, IJsonType> entry: fields.entrySet() )
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
      sb.append( "public $builderType with$suffix($propertyType $identifier) {\n" );
      indent( sb, indent );
      sb.append( "  _bindings.put(\"$key\", " ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerceToBindingValue(_bindings, $identifier));\n" );
      indent( sb, indent );
      sb.append( "  return this;\n" );
      indent( sb, indent );
      sb.append( "}\n" );
      renderUnionAccessors_With( builderType, sb, indent, key, entry.getValue() );
    }
  }

  private void renderUnionAccessors_With( @SuppressWarnings("unused") String builderType, StringBuilder sb, int indent, String key, IJsonType type )
  {
    if( isCollapsedUnionEnum( type ) )
    {
      return;
    }

    Set<IJsonType> union = _state._unionMembers.get( key );
    if( union != null )
    {
      for( IJsonType constituentType: union )
      {
        sb.append( '\n' );
        String specificPropertyType = getConstituentQn( constituentType, type );
        String unionName = makeMemberIdentifier( constituentType );
        addSourcePositionAnnotation( sb, indent + 2, key );
        if( constituentType instanceof JsonSchemaType )
        {
          addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
        }
        String identifier = addActualNameAnnotation( sb, indent + 2, key, true );
        indent( sb, indent + 2 );
        sb.append( "public $builderType with" ).append( identifier ).append( "As" ).append( unionName ).append( "(" ).append( specificPropertyType ).append( " ${'$'}value) {\n" );
        indent( sb, indent + 2 );
        sb.append( "  _bindings.put(\"$key\", " ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerceToBindingValue(_bindings, ${'$'}value));\n" );
        sb.append( "  return this;\n" );
        indent( sb, indent + 2 );
        sb.append( "}\n" );
      }
    }
  }

  private Map<String, IJsonType> getNotRequired()
  {
    Map<String, IJsonType> result = new LinkedHashMap<>();
    Set<String> allRequired = getAllRequired();
    getAllMembers().forEach( (key, value) -> {
      if( !allRequired.contains( key ) )
      {
        result.put( key, value );
      }
    } );
    return result;
  }

  private void addCreateMethod( StringBuilder sb, int indent, String typeName )
  {
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " create(" );
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    addRequiredParams( sb, allRequired, allMembers );
    sb.append( ") {\n" );
    indent( sb, indent + 2 );
    //noinspection unused
    String DataBindings = DataBindings.class.getSimpleName();
    sb.append( "$DataBindings bindings_ = new $DataBindings();\n" );
    for( String requiredProp: allRequired )
    {
      IJsonType paramType = allMembers.get( requiredProp );
      if( paramType != null && paramType.getTypeAttributes().getDefaultValue() == null )
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
        //noinspection unused
        String Json = Json.class.getSimpleName();
        indent( sb, indent + 2 );
        sb.append( "bindings_.put(\"${entry.getKey()}\", " );
        if( defaultValue instanceof Bindings )
        {
          indent( sb, indent + 2 );
          sb.append( "$Json.fromJson(\"" );
          StringBuilder defValJson = new StringBuilder();
          JsonUtil.toJson( defValJson, 0, defaultValue );
          sb.append( ManEscapeUtil.escapeForJava( defValJson.toString() ) );
          sb.append( "\")" );
        }
        else if( defaultValue instanceof List )
        {
          indent( sb, indent + 2 );
          sb.append( "$Json.fromJson(\"" );
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

  private void addRequiredParams( StringBuilder sb, Set<String> allRequired, Map<String, IJsonType> allMembers )
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

  private void addLoadMethod( StringBuilder sb, int indent, @SuppressWarnings("unused") String typeName )
  {
    indent( sb, indent );
    //noinspection unused
    String Loader = Loader.class.getTypeName();
    sb.append( "static " ).append( "$Loader<$typeName>" ).append( " load() {\n" );
    indent( sb, indent );
    sb.append( "  return new $Loader<>();\n" );
    indent( sb, indent );
    sb.append( "}\n" );
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
    if( _allRequired != null )
    {
      return _allRequired;
    }

    Set<String> allRequired = new LinkedHashSet<>();
    for( IJsonType extended: getSuperTypes() )
    {
      if( extended instanceof JsonStructureType )
      {
        allRequired.addAll( ((JsonStructureType)extended)._state._required );
      }
    }
    allRequired.addAll( _state._required  );
    return _allRequired = allRequired;
  }

  private void verifyAllRequired( Collection<String> allRequired, Token token )
  {
    for( Iterator<String> iterator = allRequired.iterator(); iterator.hasNext(); )
    {
      String req = iterator.next();
      IJsonType paramType = getAllMembers().get( req );
      if( paramType == null )
      {
        iterator.remove();
        addIssue( new JsonIssue( IIssue.Kind.Error, token,
          "Cannot resolve required property: '" + req + "' on type: " + getName() ) );
      }
    }
  }

  /** Keeping tokens so we can verify named required property exists during resolve() */
  public void addRequiredWithTokens( Object withTokens )
  {
    if( withTokens != null )
    {
      if( _state._requiredWithTokens.isEmpty() )
      {
        _state._requiredWithTokens = new LinkedHashSet<>();
      }
      _state._requiredWithTokens.add( withTokens );
    }
  }

  private void addRequired( Collection<String> required )
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
