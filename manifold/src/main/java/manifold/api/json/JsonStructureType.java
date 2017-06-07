package manifold.api.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.api.sourceprod.ActualName;

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
    
    for( Map.Entry<String, IJsonType> e: _members.entrySet() )
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
    
    for( Map.Entry<String, IJsonParentType> e: _innerTypes.entrySet() )
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

    sb.append( "structure " ).append( identifier ).append( addSuperTypes( sb ) ).append( " {\n" );
    renderTopLevelFactoryMethods( sb, indent + 2 );
    for( String key : _members.keySet() )
    {
      identifier = addActualNameAnnotation( sb, indent + 2, key );
      indent( sb, indent + 2 );
      sb.append( "property get " ).append( identifier ).append( "(): " ).append( _members.get( key ).getName() ).append( "\n" );
      if( mutable )
      {
        indent( sb, indent + 2 );
        sb.append( "property set " ).append( identifier ).append( "( " ).append( "$value: " ).append( _members.get( key ).getName() ).append( " )\n" );
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
    String identifier = Json.makeIdentifier( name );
    if( !identifier.equals( name ) )
    {
      indent( sb, indent );
      sb.append( "@" ).append( ActualName.class.getName() ).append( "( \"" ).append( name ).append( "\" )\n" );
    }
    return identifier;
  }

  private void renderTopLevelFactoryMethods( StringBuilder sb, int indent )
  {
    if( getParent() != null )
    {
      // Only add factory methods to top-level json structure
      return;
    }

    indent( sb, indent );
    sb.append( "static function fromJson( jsonText: String ): " ).append( getName() ).append( " {\n" );
    indent( sb, indent );
    sb.append( "  return gw.lang.reflect.json.Json.fromJson( jsonText ) as " ).append( getName() ).append( "\n" );
    indent( sb, indent );
    sb.append( "}\n" );
    indent( sb, indent );
    sb.append( "static function fromJsonUrl( url: String ): " ).append( getName() ).append( " {\n" );
    indent( sb, indent );
    sb.append( "  return new java.net.URL( url ).JsonContent\n" );
    indent( sb, indent );
    sb.append( "}\n" );
    indent( sb, indent );
    sb.append( "static function fromJsonUrl( url: java.net.URL ): " ).append( getName() ).append( " {\n" );
    indent( sb, indent );
    sb.append( "  return url.JsonContent\n" );
    indent( sb, indent );
    sb.append( "}\n" );
    indent( sb, indent );
    sb.append( "static function fromJsonFile( file: java.io.File ) : " ).append( getName() ).append( " {\n" );
    indent( sb, indent );
    sb.append( "  return fromJsonUrl( file.toURI().toURL() )\n" );
    indent( sb, indent );
    sb.append( "}\n" );
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
