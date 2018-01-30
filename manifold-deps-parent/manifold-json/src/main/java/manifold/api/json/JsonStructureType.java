package manifold.api.json;

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
import manifold.util.ManStringUtil;

/**
 */
public class JsonStructureType extends JsonSchemaType
{
  private static final String FIELD_FILE_URL = "__FILE_URL_";
  private List<IJsonParentType> _superTypes;
  private Map<String, IJsonType> _membersByName;
  private Map<String, Set<IJsonType>> _unionMembers;
  private Map<String, Token> _memberLocations;
  private Map<String, IJsonParentType> _innerTypes;
  private Token _token;

  public JsonStructureType( JsonSchemaType parent, URL source, String name )
  {
    super( name, source, parent );
    _membersByName = new HashMap<>();
    _memberLocations = new HashMap<>();
    _innerTypes = Collections.emptyMap();
    _unionMembers = Collections.emptyMap();
    _superTypes = Collections.emptyList();
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

  public void addSuper( IJsonParentType superType )
  {
    if( _superTypes.isEmpty() )
    {
      _superTypes = new ArrayList<>();
    }
    _superTypes.add( superType );
  }
  public List<IJsonParentType> getSuperTypes()
  {
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
          throw new RuntimeException( "Types disagree for '" + name + "' from array data: " + type.getName() + " vs: " + existingType.getName() );
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

  public IJsonType findMemberType( String name )
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

  JsonStructureType merge( JsonStructureType other )
  {
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
    if( getParent() instanceof IJsonParentType )
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
      String propertyType = type instanceof JsonUnionType ? "Object" : type.getIdentifier();
      addSourcePositionAnnotation( sb, indent + 2, key );
      identifier = addActualNameAnnotation( sb, indent + 2, key, true );
      indent( sb, indent + 2 );
      sb.append( propertyType ).append( " get" ).append( identifier ).append( "();\n" );
      if( mutable )
      {
        addSourcePositionAnnotation( sb, indent + 2, key );
        addActualNameAnnotation( sb, indent + 2, key, true );
        indent( sb, indent + 2 );
        sb.append( "void set" ).append( identifier ).append( "(" ).append( propertyType ).append( " $value);\n" );
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
          sb.append( specificPropertyType ).append( " get" ).append( identifier ).append( "As" ).append( unionName ).append( "();\n" );
          if( mutable )
          {
            addSourcePositionAnnotation( sb, indent + 2, key );
            if( constituentType instanceof JsonSchemaType )
            {
              addTypeReferenceAnnotation( sb, indent + 2, (JsonSchemaType)getConstituentQnComponent( constituentType ) );
            }
            addActualNameAnnotation( sb, indent + 2, key, true );
            indent( sb, indent + 2 );
            sb.append( "void set" ).append( identifier ).append( "As" ).append( unionName ).append( "(" ).append( specificPropertyType ).append( " $value);\n" );
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
      qn = "java.util.List<" + qn + ">";
      propertyType = ((JsonListType)propertyType).getComponentType();
    }
    return qn;
  }

  private String getConstituentQn( IJsonType constituentType )
  {
    if( constituentType instanceof JsonListType )
    {
      return "java.util.List<" + getConstituentQn( ((JsonListType)constituentType).getComponentType() ) + ">";
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
    indent( sb, indent );
    sb.append( "String " + FIELD_FILE_URL + " = \"" ).append( getFile().toString() ).append( "\";\n" );
  }

  private String addSuperTypes( StringBuilder sb )
  {
    List<IJsonParentType> superTypes = getSuperTypes();
    if( superTypes.isEmpty() )
    {
      return "";
    }

    sb.append( " extends " );
    for( int i = 0; i < superTypes.size(); i++ )
    {
      IJsonParentType superType = superTypes.get( i );
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( superType.getIdentifier() );
    }
    return "";
  }

  private String addActualNameAnnotation( StringBuilder sb, int indent, String name, boolean capitalize )
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
    if( type instanceof JsonListType )
    {
      return "ListOf" + makeMemberIdentifier( ((JsonListType)type).getComponentType() );
    }
    return makeIdentifier( type.getName(), false );
  }
  private String makeIdentifier( String name, boolean capitalize )
  {
    return capitalize ? ManStringUtil.capitalize( JsonUtil.makeIdentifier( name ) ) : JsonUtil.makeIdentifier( name );
  }

  private boolean addSourcePositionAnnotation( StringBuilder sb, int indent, String name )
  {
    Token token = _memberLocations.get( name );
    if( token == null )
    {
      return false;
    }
    return addSourcePositionAnnotation( sb, indent, name, token );
  }
  private boolean addSourcePositionAnnotation( StringBuilder sb, int indent, String name, Token token )
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
      .addArgument( "value", String.class, type.getIdentifier() );
    annotation.render( sb, indent );
  }

  private void renderTopLevelFactoryMethods( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    String typeName = getIdentifier();
    sb.append( "static " ).append( typeName ).append( " create() {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( typeName ).append( ")new javax.script.SimpleBindings();\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    // These are all implemented by Bindings via ManBindingsExt
    indent( sb, indent );
    sb.append( "default String" ).append( " toJson() {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toJson(this);\n" );
    indent( sb, indent );
    sb.append( "}\n");

    indent( sb, indent );
    sb.append( "default String" ).append( " toXml() {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toXml(this);\n" );
    indent( sb, indent );
    sb.append( "}\n");

    indent( sb, indent );
    sb.append( "default String" ).append( " toXml(String name) {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toXml(this, name);\n" );
    indent( sb, indent );
    sb.append( "}\n");

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

  private void indent( StringBuilder sb, int indent )
  {
    for( int i = 0; i < indent; i++ )
    {
      sb.append( ' ' );
    }
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
    result = 31 * result + _membersByName.hashCode();
    result = 31 * result + _unionMembers.hashCode();
    result = 31 * result + _innerTypes.hashCode();
    return result;
  }

  public String toString()
  {
    return getFqn();
  }
}
