package manifold.api.json.schema;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import manifold.api.json.DynamicType;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonSchemaType;
import manifold.api.json.JsonSimpleType;
import manifold.api.json.JsonStructureType;
import manifold.util.cache.FqnCache;

/**
 */
public class JsonSchemaTransformer
{
  private static final String JSCH_SCHEMA = "$schema";
  private static final String JSCH_TYPE = "type";
  private static final String JSCH_NAME = "name";
  private static final String JSCH_REF = "$ref";
  private static final String JSCH_DEFINITIONS = "definitions";
  private static final String JSCH_PROPERTIES = "properties";
  private static final String JSCH_ENUM = "enum";
  private static final String JSCH_ALL_OF = "allOf";
  private static final String JSCH_ONE_OF = "oneOf";

  private static final Map<URI, FqnCache<IJsonType>> _typesByUri = new ConcurrentHashMap<>();

  private FqnCache<IJsonType> _typeByFqn;

  private JsonSchemaTransformer()
  {
    _typeByFqn = new FqnCache<>( "doc", true, Json::makeIdentifier );
  }

  public static boolean isSchema( Bindings bindings )
  {
    return bindings.get( JsonSchemaTransformer.JSCH_SCHEMA ) != null;
  }

  public static IJsonType transform( String name, Bindings docObj )
  {
    assertSchema( docObj );

    JsonSchemaTransformer transformer = new JsonSchemaTransformer();

    List<IJsonType> definitions = transformer.transformDefinitions( docObj );

    name = name == null || name.isEmpty() ? (String)docObj.get( JSCH_NAME ) : name;
    IJsonType type = transformer.transformType( null, name, docObj );
    type.setDefinitions( definitions );
    return type;
  }

  private List<IJsonType> transformDefinitions( Bindings docObj )
  {
    Bindings definitions = (Bindings)docObj.get( JSCH_DEFINITIONS );
    if( definitions == null )
    {
      return null;
    }

    JsonStructureType definitionsHolder = new JsonStructureType( null, JSCH_DEFINITIONS );
    List<IJsonType> result = new ArrayList<>();
    cache( definitionsHolder );
    for( Map.Entry<String, Object> entry: definitions.entrySet() )
    {
      String name = entry.getKey();
      Bindings value = (Bindings)entry.getValue();
      IJsonType type = transformType( definitionsHolder, name, value );
      result.add( type );
    }
    return result;
  }

  private IJsonType findRef( String localRef )
  {
    localRef = localRef.replace( '/', '.' );
    char firstChar = localRef.charAt( 0 );
    if( firstChar == '.' || firstChar == '#' )
    {
      localRef = localRef.substring( 1 );
    }
    return _typeByFqn.get( localRef );
  }

  void cache( JsonSchemaType type )
  {
    _typeByFqn.add( makeFqn( type ), type );
  }

  private String makeFqn( JsonSchemaType type )
  {
    String result = "";
    if( type.getParent() != null )
    {
      result = makeFqn( type.getParent() );
      result += '.';
    }
    return result + type.getLabel();
  }

  private static void assertSchema( Bindings docObj )
  {
    if( docObj.get( JSCH_SCHEMA ) == null )
    {
      throw new IllegalArgumentException( "The Json object does not contain a '$schema' element." );
    }
  }

  IJsonType transformType( JsonSchemaType parent, String name, Bindings jsonObj )
  {
    IJsonType result;
    String type = (String)jsonObj.get( JSCH_TYPE );
    if( type == null )
    {
      result = findReference( jsonObj );
      if( result == null )
      {
        result = transformCombination( parent, name, jsonObj );
        if( result == null )
        {
          result = deriveTypeFromEnum( jsonObj );
          if( result == null )
          {
            // No type or other means of deriving a type could be found.
            // Default type is Dynamic (in Java this is a Bindings Object)
            result = DynamicType.instance();
          }
        }
      }
      return result;
    }

    if( jsonObj.get( JSCH_ONE_OF ) != null )
    {
      // "oneOf" is difficult to handle for Java's type system,
      // especially with $ref it basically implies union types, which Java does not support.
      // So for now we punt and deal with it as a dynamic type, which Java does not support,
      // but we will approximate it with Bindings (a map)
      return DynamicType.instance();
    }

    switch( Type.fromName( type ) )
    {
      case Object:
        result = ObjectTransformer.transform( this, name, parent, jsonObj );
        break;
      case Array:
        result = ArrayTransformer.transform( this, name, parent, jsonObj );
        break;
      case String:
        result = JsonSimpleType.String;
        break;
      case Number:
        result = JsonSimpleType.Double;
        break;
      case Integer:
        result = JsonSimpleType.Integer;
        break;
      case Boolean:
        result = JsonSimpleType.Boolean;
        break;
      case Dynamic:
      case Null:
        result = DynamicType.instance();
        break;
      default:
        throw new IllegalStateException( "Unhandled type: " + type );
    }
    return result;
  }

  private IJsonType deriveTypeFromEnum( Bindings bindings )
  {
    List list = (List)bindings.get( JSCH_ENUM );
    if( list == null )
    {
      return null;
    }

    IJsonType type = null;
    for( Object elem: list )
    {
      IJsonType csr = Json.transformJsonObject( "", null, elem );
      if( type == null )
      {
        type = csr;
      }
      else if( !type.equals( csr ) )
      {
        type = DynamicType.instance();
      }
    }

    return type;
  }

  private IJsonType transformCombination( JsonSchemaType parent, String name, Bindings jsonObj )
  {
    List list = (List)jsonObj.get( JSCH_ALL_OF );
    if( list == null )
    {
      return null;
    }

    JsonStructureType type = buildHierarchy( parent, name, list );
    if( type != null )
    {
      for( Object elem: list )
      {
        if( elem instanceof Bindings )
        {
          Bindings elemBindings = (Bindings)elem;
          Bindings properties = (Bindings)elemBindings.get( JSCH_PROPERTIES );
          if( properties != null )
          {
            ObjectTransformer.transform( this, type, elemBindings );
          }
          break;
        }
      }
    }

    return type;
  }

  private JsonStructureType buildHierarchy( JsonSchemaType parent, String name, List list )
  {
    JsonStructureType type = null;
    for( Object elem: list )
    {
      if( elem instanceof Bindings )
      {
        Bindings elemBindings = (Bindings)elem;
        IJsonType ref = findReference( elemBindings );
        if( ref != null )
        {
          if( type == null )
          {
            type = new JsonStructureType( parent, name );
          }
          type.addSuper( (IJsonParentType)ref );
        }
      }
    }
    return type;
  }

  private IJsonType findReference( Bindings jsonObj )
  {
    String ref = (String)jsonObj.get( JSCH_REF );
    if( ref == null )
    {
      return null;
    }

    try
    {
      URI uri = new URI( ref );
      String scheme = uri.getSchemeSpecificPart();
      if( scheme != null )
      {
        //## todo: handle absolute URI
        // What we want here is to handle only other resource files.  Nothing else.
        // So if there is an absolute URI, it must be to another resource file and
        // it must be a Java resource path address "abc/foo/My.json"
        // This way we can use the type system to resolve there.
      }

      String fragment = uri.getFragment();
      if( fragment != null )
      {
        return findRef( fragment );
      }
      throw new UnsupportedOperationException( "Unhandled URI: " + ref );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
