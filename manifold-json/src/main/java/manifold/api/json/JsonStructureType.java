package manifold.api.json;

import extensions.java.net.URL.ManUrlExt;
import extensions.javax.script.Bindings.ManBindingsExt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.type.ActualName;
import manifold.api.type.SourcePosition;
import manifold.util.JsonUtil;
import manifold.util.ManStringUtil;

/**
 */
public class JsonStructureType extends JsonSchemaType
{
  private static final String FIELD_FILE_URL = "__FILE_URL_";
  private List<IJsonParentType> _superTypes;
  private Map<String, IJsonType> _members;
  private Map<String, Token> _memberLocations;
  private Map<String, IJsonParentType> _innerTypes;
  private Token _token;

  public JsonStructureType( JsonSchemaType parent, String name )
  {
    super( name, parent );
    _members = new HashMap<>();
    _memberLocations = new HashMap<>();
    _innerTypes = new HashMap<>();
    _superTypes = new ArrayList<>();
  }

  public void addSuper( IJsonParentType superType )
  {
    _superTypes.add( superType );
  }

  public List<IJsonParentType> getSuperTypes()
  {
    return _superTypes;
  }

  public void addChild( String name, IJsonParentType type )
  {
    _innerTypes.put( name, type );
  }

  public IJsonParentType findChild( String name )
  {
    IJsonParentType innerType = _innerTypes.get( name );
    if( innerType == null )
    {
      List<IJsonType> definitions = getDefinitions();
      if( definitions != null )
      {
        for( IJsonType child: definitions )
        {
          if( child.getName().equals( name ) )
          {
            innerType = (IJsonParentType)child;
            break;
          }
        }
      }
    }
    return innerType;
  }

  public Map<String, IJsonType> getMembers()
  {
    return _members;
  }

  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _innerTypes;
  }

  public void addMember( String name, IJsonType type, Token token )
  {
    IJsonType existingType = _members.get( name );
    if( existingType != null && existingType != type )
    {
      if( type == DynamicType.instance() )
      {
        // Keep the more specific type (the dynamic type was inferred form a 'null' value, which should not override a static type)
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
    _members.put( name, type );
    _memberLocations.put( name, token );
  }

  public IJsonType findMemberType( String name )
  {
    return _members.get( name );
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

    JsonStructureType mergedType = new JsonStructureType( getParent(), getName() );

    for( Map.Entry<String, IJsonType> e : _members.entrySet() )
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
    indent( sb, indent );

    String name = getName();
    String identifier = addActualNameAnnotation( sb, indent, name, false );

    if( !(getParent() instanceof JsonStructureType) ||
        !((JsonStructureType)getParent()).addSourcePositionAnnotation( sb, indent + 2, identifier ) )
    {
      if( getToken() != null )
      {
        // this is most likely a "definitions" inner class
        addSourcePositionAnnotation( sb, indent + 2, identifier, getToken() );
      }
    }
    indent( sb, indent );
    sb.append( "@Structural\n" );
    indent( sb, indent );
    sb.append( "public interface " ).append( identifier ).append( addSuperTypes( sb ) ).append( " {\n" );
    renderFileField( sb, indent + 2 );
    renderTopLevelFactoryMethods( sb, indent + 2 );
    for( String key : _members.keySet() )
    {
      String propertyType = _members.get( key ).getIdentifier();
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

  private void renderFileField( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( "String " + FIELD_FILE_URL + " = \"" ).append( getFile().toString() ).append( "\";\n" );
  }

  private String addSuperTypes( StringBuilder sb )
  {
    for( int i = 0; i < _superTypes.size(); i++ )
    {
      IJsonParentType superType = _superTypes.get( i );
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
    String identifier = capitalize ? ManStringUtil.capitalize( JsonUtil.makeIdentifier( name ) ) : JsonUtil.makeIdentifier( name );
    if( !identifier.equals( name ) )
    {
      indent( sb, indent );
      sb.append( "@" ).append( ActualName.class.getName() ).append( "( \"" ).append( name ).append( "\" )\n" );
    }
    return identifier;
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
    indent( sb, indent );
    SrcAnnotationExpression annotation = new SrcAnnotationExpression( SourcePosition.class.getName() )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( getName(), FIELD_FILE_URL ) ).name( "url" ) )
      .addArgument( "feature", String.class, name )
      .addArgument( "offset", int.class, token.getOffset() )
      .addArgument( "length", int.class, name.length() );
    annotation.render( sb, indent );
    sb.append( '\n' );
    return true;
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
    sb.append( "};\n");

    indent( sb, indent );
    sb.append( "default String" ).append( " toXml() {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toXml(this);\n" );
    indent( sb, indent );
    sb.append( "};\n");

    indent( sb, indent );
    sb.append( "default String" ).append( " toXml(String name) {\n" );
    indent( sb, indent );
    sb.append( "  return " ).append( ManBindingsExt.class.getName() ).append( ".toXml(this, name);\n" );
    indent( sb, indent );
    sb.append( "};\n");

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
    if( !_members.equals( type._members ) )
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
    result = 31 * result + _members.hashCode();
    result = 31 * result + _innerTypes.hashCode();
    return result;
  }
}
