package manifold.api.json;

import extensions.java.net.URL.ManUrlExt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.api.sourceprod.ActualName;
import manifold.util.JsonUtil;
import manifold.util.ManStringUtil;

/**
 */
public class JsonStructureType extends JsonSchemaType
{
  private List<IJsonParentType> _superTypes;
  private Map<String, IJsonType> _members;
  private Map<String, IJsonParentType> _innerTypes;

  public JsonStructureType( JsonSchemaType parent, String name )
  {
    super( name, parent );
    _members = new HashMap<>();
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
    return _innerTypes.get( name );
  }

  public Map<String, IJsonType> getMembers()
  {
    return _members;
  }

  public Map<String, IJsonParentType> getInnerTypes()
  {
    return _innerTypes;
  }

  public void addMember( String name, IJsonType type )
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
  }

  public IJsonType findMemberType( String name )
  {
    return _members.get( name );
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
        mergedType.addMember( memberName, memberType );
      }
      else
      {
        return null;
      }
    }

    for( Map.Entry<String, IJsonParentType> e : _innerTypes.entrySet() )
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
        return null;
      }
    }

    return mergedType;
  }

  public void render( StringBuilder sb, int indent, boolean mutable )
  {
    indent( sb, indent );

    String name = getName();
    String identifier = addActualNameAnnotation( sb, indent, name );

    sb.append( "@Structural\n" );
    indent( sb, indent );
    sb.append( "public interface " ).append( identifier ).append( addSuperTypes( sb ) ).append( " {\n" );
    renderTopLevelFactoryMethods( sb, indent + 2 );
    for( String key : _members.keySet() )
    {
      String propertyType = _members.get( key ).getName();
      identifier = ManStringUtil.capitalize( addActualNameAnnotation( sb, indent + 2, key ) );
      indent( sb, indent + 2 );
      sb.append( propertyType ).append( " get" ).append( identifier ).append( "();\n" );
      if( mutable )
      {
        indent( sb, indent + 2 );
        sb.append( "void set" ).append( identifier ).append( "(" ).append( propertyType ).append( " $value);\n" );
      }
    }
    for( IJsonParentType child : _innerTypes.values() )
    {
      child.render( sb, indent + 2, mutable );
    }
    indent( sb, indent );
    sb.append( "}\n" );
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
      sb.append( superType.getName() );
    }
    return "";
  }

  private String addActualNameAnnotation( StringBuilder sb, int indent, String name )
  {
    String identifier = JsonUtil.makeIdentifier( name );
    if( !identifier.equals( name ) )
    {
      indent( sb, indent );
      sb.append( "@" ).append( ActualName.class.getName() ).append( "( \"" ).append( name ).append( "\" )\n" );
    }
    return identifier;
  }

  private void renderTopLevelFactoryMethods( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( "static " ).append( getName() ).append( " create() {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( getName() ).append( ")new javax.script.SimpleBindings();\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    if( !shouldRenderTopLevel( this ) )
    {
      // Only add factory methods to top-level json structure
      return;
    }

    indent( sb, indent );
    sb.append( "static " ).append( getName() ).append( " fromJson(String jsonText) {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( getName() ).append( ")" ).append( Json.class.getName() ).append( ".fromJson(jsonText);\n" );
    indent( sb, indent );
    sb.append( "}\n" );
    indent( sb, indent );
    sb.append( "static " ).append( getName() ).append( " fromJsonUrl(String url) {\n" );
    indent( sb, indent );
    sb.append( "  try {\n" );
    indent( sb, indent );
    sb.append( "    return (" ).append( getName() ).append( ")" ).append( ManUrlExt.class.getName() ).append( ".getJsonContent(new java.net.URL(url));\n" );
    indent( sb, indent );
    sb.append( "  } catch(Exception e) {throw new RuntimeException(e);}\n" );
    indent( sb, indent );
    sb.append( "}\n" );
    indent( sb, indent );
    sb.append( "static " ).append( getName() ).append( " fromJsonUrl(java.net.URL url) {\n" );
    indent( sb, indent );
    sb.append( "  return (" ).append( getName() ).append( ")" ).append( ManUrlExt.class.getName() ).append( ".getJsonContent(url);\n" );
    indent( sb, indent );
    sb.append( "}\n" );
    indent( sb, indent );
    sb.append( "static " ).append( getName() ).append( " fromJsonFile(java.io.File file) {\n" );
    indent( sb, indent );
    sb.append( "  try {\n" );
    indent( sb, indent );
    sb.append( "    return (" ).append( getName() ).append( ")fromJsonUrl(file.toURI().toURL());\n" );
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
