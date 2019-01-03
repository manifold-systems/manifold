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

import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.script.Bindings;
import manifold.api.json.schema.JsonEnumType;
import manifold.api.json.schema.LazyRefJsonType;
import manifold.ext.RuntimeMethods;
import manifold.ext.api.IBindingsBacked;
import manifold.json.extensions.java.net.URL.ManUrlExt;
import manifold.json.extensions.javax.script.Bindings.ManBindingsExt;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
  private List<IJsonType> _superTypes;
  private Map<String, IJsonType> _membersByName;
  private Map<String, Set<IJsonType>> _unionMembers;
  private Map<String, Token> _memberLocations;
  private Map<String, IJsonParentType> _innerTypes;
  private Set<String> _required;
  private Token _token;

  public JsonStructureType( JsonSchemaType parent, URL source, String name )
  {
    super( name, source, parent );

    // Using LinkedHashMap to preserve insertion order, an impl detail currently required by the IJ plugin for rename
    // refactoring i.e., renaming a json property should result in a source file that differs only in the naming
    // difference -- there should be no difference in ordering of methods etc.
    _membersByName = new LinkedHashMap<>();
    _memberLocations = new LinkedHashMap<>();

    _innerTypes = Collections.emptyMap();
    _unionMembers = Collections.emptyMap();
    _superTypes = Collections.emptyList();
    _required = Collections.emptySet();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();
    for( Map.Entry<String, IJsonType> entry: new HashSet<>( _membersByName.entrySet() ) )
    {
      IJsonType type = entry.getValue();

      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        _membersByName.put( entry.getKey(), ((LazyRefJsonType)type).resolve() );
      }
    }
    for( Map.Entry<String, Set<IJsonType>> entry: new HashSet<>( _unionMembers.entrySet() ) )
    {
      Set<IJsonType> types = new HashSet<>();
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
      _unionMembers.put( entry.getKey(), types );
    }
    for( Map.Entry<String, IJsonParentType> entry: new HashSet<>( _innerTypes.entrySet() ) )
    {
      IJsonType type = entry.getValue();
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        type = ((LazyRefJsonType)type).resolve();
        _innerTypes.put( entry.getKey(), (IJsonParentType)type );
      }
    }
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
    if( _superTypes.isEmpty() )
    {
      _superTypes = new ArrayList<>();
    }
    _superTypes.add( superType );
  }
  private List<IJsonType> getSuperTypes()
  {
    if( !_superTypes.isEmpty() )
    {
      if( _superTypes.stream().anyMatch( e -> e instanceof LazyRefJsonType ) )
      {
        List<IJsonType> resolved = new ArrayList<>();
        for( IJsonType type: _superTypes )
        {
          if( type instanceof LazyRefJsonType )
          {
            type = ((LazyRefJsonType)type).resolve();
          }
          resolved.add( type );
        }
        _superTypes = resolved;
      }
    }
    return _superTypes;
  }

  public void addChild( String name, IJsonParentType type )
  {
    if( _innerTypes.isEmpty() )
    {
      _innerTypes = new HashMap<>();
    }
    _innerTypes.put( name, type );
  }

  public IJsonType findChild( String name )
  {
    // look in inner types
    IJsonParentType innerType = _innerTypes.get( name );

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
      for( Set<IJsonType> constituents: _unionMembers.values() )
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
    return _membersByName;
  }
  private Map<String, IJsonType> getAllMembers()
  {
    Map<String, IJsonType> allMembers = new HashMap<>( _membersByName );
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
    return _memberLocations;
  }

  @SuppressWarnings("unused")
  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _innerTypes;
  }

  public void addMember( String name, IJsonType type, Token token )
  {
    IJsonType existingType = _membersByName.get( name );
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
    _membersByName.put( name, type );
    _memberLocations.put( name, token );
    addUnionMemberAccess( name, type );
  }

  private void addUnionMemberAccess( String name, IJsonType type )
  {
    if( type instanceof JsonUnionType )
    {
      for( IJsonType constituent : ((JsonUnionType)type).getConstituents() )
      {
        if( _unionMembers.isEmpty() )
        {
          _unionMembers = new HashMap<>();
        }

        Set<IJsonType> union = _unionMembers.computeIfAbsent( name, k -> new HashSet<>() );
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
    return _membersByName.get( name );
  }

  public Token getToken()
  {
    return _token;
  }
  public void setToken( Token token )
  {
    _token = token;
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

    JsonStructureType mergedType = new JsonStructureType( getParent(), getFile(), getName() );

    for( Map.Entry<String, IJsonType> e : _membersByName.entrySet() )
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
        mergedType.addMember( memberName, memberType, _memberLocations.get( memberName ) );
      }
      else
      {
        return null;
      }
    }

    if( !mergeInnerTypes( other, mergedType, _innerTypes ) )
    {
      return null;
    }

    return mergedType;
  }

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
    sb.append( "@Structural\n" );
    indent( sb, indent );
    sb.append( "public interface " ).append( identifier ).append( addSuperTypes( sb ) ).append( " {\n" );
    renderFileField( sb, indent + 2 );
    renderTopLevelFactoryMethods( sb, indent + 2 );
    for( String key : _membersByName.keySet() )
    {
      sb.append( '\n' );
      IJsonType type = _membersByName.get( key );
      String propertyType = getPropertyType( type );
      addSourcePositionAnnotation( sb, indent + 2, key );
      identifier = addActualNameAnnotation( sb, indent + 2, key, true );
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
      if( mutable )
      {
        addSourcePositionAnnotation( sb, indent + 2, key );
        addActualNameAnnotation( sb, indent + 2, key, true );
        indent( sb, indent + 2 );
        sb.append( "default void set" ).append( identifier ).append( "(" ).append( propertyType ).append( " ${'$'}value) {\n" );
        indent( sb, indent + 4 );
        sb.append( "getBindings().put(\"$key\", " ).append( RuntimeMethods.class.getSimpleName() ).append( ".coerceToBindingValue(getBindings(), ${'$'}value));\n" );
        indent( sb, indent + 2 );
        sb.append( "}\n" );
      }
      Set<IJsonType> union = _unionMembers.get( key );
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
    for( IJsonParentType child : _innerTypes.values() )
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
    indent( sb, indent );
    sb.append( "}\n" );
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
    if( propertyType instanceof JsonListType )
    {
      return List.class.getTypeName() + '<' + getPropertyType( ((JsonListType)propertyType).getComponentType() ) + '>';
    }
    if( propertyType instanceof JsonUnionType )
    {
      return Object.class.getSimpleName();
    }
    return propertyType.getIdentifier();
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
      sb.append( ", " );
      sb.append( superType.getIdentifier() );
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
    Token token = _memberLocations.get( name );
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

  private void renderTopLevelFactoryMethods( StringBuilder sb, int indent )
  {
    String typeName = getIdentifier();

    // Add a static create(...) method having parameters corresonding with "required" properties not having
    // a "default" value.
    addCreateMethod( sb, indent, typeName );

    // Called reflectively from RuntimeMethods, this proxy and the default get/set method impls defined here enable the
    // JSON type manifold to avoid the overhead of dynamic proxy generation and compilation at runtime. Otherwise the
    // ICallHandler-based dynamic proxy would be used, which causes a significant delay the first time a JSON interface
    // is used.
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " proxy(" ).append( Bindings.class.getSimpleName() ).append( " bindings) {\n" );
    sb.append( "    return new $typeName() {\n" +
               "      public Bindings getBindings() {\n" +
               "        return bindings;\n" +
               "      }\n" +
               "    };\n" );
    indent( sb, indent );
    sb.append( "}\n" );

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

    if( !shouldRenderTopLevel( this ) )
    {
      // Only add factory methods to top-level json structure
      return;
    }

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

  private void addCreateMethod( StringBuilder sb, int indent, String typeName )
  {
    indent( sb, indent );
    sb.append( "static " ).append( typeName ).append( " create(" );
    int count = 0;
    Set<String> allRequired = getAllRequired();
    Map<String, IJsonType> allMembers = getAllMembers();
    for( String param: allRequired )
    {
      IJsonType paramType = allMembers.get( param );
      if( paramType.getDefaultValue() == null )
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
    sb.append( ") {\n" );
    indent( sb, indent );

    sb.append( "SimpleBindings bindings_ = new SimpleBindings();\n" );
    indent( sb, indent );
    for( String requiredProp: allRequired )
    {
      IJsonType paramType = allMembers.get( requiredProp );
      if( paramType.getDefaultValue() == null )
      {
        indent( sb, indent );
        //noinspection unused
        String passedInParam = makeIdentifier( requiredProp, false );
        sb.append( "bindings_.put(\"$requiredProp\", $passedInParam);\n" );
      }
    }
    for( Map.Entry<String, IJsonType> entry: allMembers.entrySet() )
    {
      Object defaultValue = entry.getValue().getDefaultValue();
      if( defaultValue != null )
      {
        indent( sb, indent + 2 );
        sb.append( "bindings_.put(\"${entry.getKey()}\", " );
        if( defaultValue instanceof Bindings )
        {
          sb.append( "Json.fromJson(\"" );
          StringBuilder defValJson = new StringBuilder();
          JsonUtil.toJson( defValJson, 0, defaultValue );
          sb.append( ManEscapeUtil.escapeForJava( defValJson.toString() ) );
          sb.append( "\")" );
        }
        else if( defaultValue instanceof List )
        {
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
    if( _superTypes.size() != that._superTypes.size() ||
        !_superTypes.stream().allMatch( t -> that._superTypes.get( i[0]++ ).equalsStructurally( t ) ) )
    {
      return false;
    }
    if( !(_membersByName.size() == that._membersByName.size() &&
          _membersByName.keySet().stream().allMatch(
            key -> that._membersByName.containsKey( key ) &&
                   _membersByName.get( key ).equalsStructurally( that._membersByName.get( key ) ) )) )
    {
      return false;
    }
    if( !(_unionMembers.size() == that._unionMembers.size() &&
          _unionMembers.keySet().stream().allMatch(
            key -> that._unionMembers.containsKey( key ) &&
                   typeSetsSame( _unionMembers.get( key ), that._unionMembers.get( key ) ) )) )
    {
      return false;
    }
    return _innerTypes.size() == that._innerTypes.size() &&
           _innerTypes.keySet().stream().allMatch(
             key -> that._innerTypes.containsKey( key ) &&
                    _innerTypes.get( key ).equalsStructurally( that._innerTypes.get( key ) ) );
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
    if( _required.contains( name ) )
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
    Set<String> allRequired = new HashSet<>( _required );
    for( IJsonType extended: getSuperTypes() )
    {
      if( extended instanceof JsonStructureType )
      {
        allRequired.addAll( ((JsonStructureType)extended)._required );
      }
    }
    return allRequired;
  }
  public void addRequired( Set<String> required )
  {
    if( _required.isEmpty() )
    {
      _required = new HashSet<>();
    }
    _required.addAll( required );
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

    if( !_superTypes.equals( type._superTypes ) )
    {
      return false;
    }
    if( !_membersByName.equals( type._membersByName ) )
    {
      return false;
    }
    if( !_unionMembers.equals( type._unionMembers ) )
    {
      return false;
    }
    return _innerTypes.equals( type._innerTypes );
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _superTypes.hashCode();
    result = 31 * result + _membersByName.keySet().hashCode();
    result = 31 * result + _unionMembers.keySet().hashCode();
    return result;
  }

  public String toString()
  {
    return getFqn();
  }
}
