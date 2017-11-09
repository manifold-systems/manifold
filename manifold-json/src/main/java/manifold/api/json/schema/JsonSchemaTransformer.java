package manifold.api.json.schema;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Bindings;
import manifold.api.json.DynamicType;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonSchemaType;
import manifold.api.json.JsonSimpleType;
import manifold.api.json.JsonStructureType;
import manifold.api.json.Token;
import manifold.util.JsonUtil;
import manifold.util.Pair;
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
    _typeByFqn = new FqnCache<>( "doc", true, JsonUtil::makeIdentifier );
  }

  public static boolean isSchema( Bindings bindings )
  {
    return bindings.get( JsonSchemaTransformer.JSCH_SCHEMA ) != null;
  }

  public static IJsonType transform( String name, Bindings docObj )
  {
    return transform( name, null, docObj );
  }
  public static IJsonType transform( String name, URL source, Bindings docObj )
  {
    assertSchema( docObj );

    JsonSchemaTransformer transformer = new JsonSchemaTransformer();

    List<IJsonType> definitions = transformer.transformDefinitions( docObj );
    if( definitions != null )
    {
      for( IJsonType def : definitions )
      {
        if( def instanceof JsonSchemaType )
        {
          ((JsonSchemaType)def).setFile( source );
        }
      }
    }

    name = name == null || name.isEmpty() ? getJSchema_Name( docObj ) : name;
    IJsonType type = transformer.transformType( null, name, docObj );
    type.setDefinitions( definitions );
    if( type instanceof JsonSchemaType )
    {
      ((JsonSchemaType)type).setFile( source );
    }
    return type;
  }

  private static String getJSchema_Name( Bindings docObj )
  {
    Object value = docObj.get( JSCH_NAME );
    String name;
    if( value instanceof Pair )
    {
      name = (String)((Pair)value).getSecond();
    }
    else
    {
      name = (String)value;
    }
    return name;
  }

  private static String getJSchema_Ref( Bindings docObj )
  {
    Object value = docObj.get( JSCH_REF );
    String name;
    if( value instanceof Pair )
    {
      name = (String)((Pair)value).getSecond();
    }
    else
    {
      name = (String)value;
    }
    return name;
  }

  private static Bindings getJSchema_Definitions( Bindings docObj )
  {
    Object value = docObj.get( JSCH_DEFINITIONS );
    return getBindings( value );
  }

  private static Bindings getBindings( Object value )
  {
    Bindings bindings;
    if( value instanceof Pair )
    {
      bindings = (Bindings)((Pair)value).getSecond();
    }
    else
    {
      bindings = (Bindings)value;
    }
    return bindings;
  }

  private static Bindings getJSchema_Properties( Bindings docObj )
  {
    Object value = docObj.get( JSCH_PROPERTIES );
    return getBindings( value );
  }

  private static List getJSchema_Enum( Bindings docObj )
  {
    Object value = docObj.get( JSCH_ENUM );
    List list;
    if( value instanceof Pair )
    {
      list = (List)((Pair)value).getSecond();
    }
    else
    {
      list = (List)value;
    }
    return list;
  }

  private static List getJSchema_AllOf( Bindings docObj )
  {
    Object value = docObj.get( JSCH_ALL_OF );
    List list;
    if( value instanceof Pair )
    {
      list = (List)((Pair)value).getSecond();
    }
    else
    {
      list = (List)value;
    }
    return list;
  }

  private List<IJsonType> transformDefinitions( Bindings docObj )
  {
    Bindings definitions = getJSchema_Definitions( docObj );
    if( definitions == null )
    {
      return null;
    }

    JsonStructureType definitionsHolder = new JsonStructureType( null, JSCH_DEFINITIONS );
    List<IJsonType> result = new ArrayList<>();
    cache( definitionsHolder );
    for( Map.Entry<String, Object> entry : definitions.entrySet() )
    {
      String name = entry.getKey();
      Bindings value = getBindings( entry.getValue() );
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
    return result + JsonUtil.makeIdentifier( type.getLabel() );
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
    Object value = jsonObj.get( JSCH_TYPE );
    String type;
    Token token = null;
    if( value instanceof Pair )
    {
      type = (String)((Pair)value).getSecond();
      token = (Token)((Pair)value).getFirst();
    }
    else
    {
      type = (String)value;
    }

    if( type == null )
    {
      return findReferenceType( parent, name, jsonObj );
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
        if( jsonObj.get( JSCH_REF ) != null )
        {
          result = findReferenceType( parent, name, jsonObj );
        }
        else
        {
          result = ObjectTransformer.transform( this, name, parent, jsonObj );
        }
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
      case Invalid:
      default:
        throw new IllegalSchemaTypeName( type, token );

    }
    return result;
  }

  private IJsonType findReferenceType( JsonSchemaType parent, String name, Bindings jsonObj )
  {
    IJsonType result;
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

  private IJsonType deriveTypeFromEnum( Bindings bindings )
  {
    List list = getJSchema_Enum( bindings );
    if( list == null )
    {
      return null;
    }

    IJsonType type = null;
    for( Object elem : list )
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
    List list = getJSchema_AllOf( jsonObj );
    if( list == null )
    {
      return null;
    }

    JsonStructureType type = buildHierarchy( parent, name, list );
    if( type != null )
    {
      for( Object elem : list )
      {
        if( elem instanceof Pair )
        {
          elem = ((Pair)elem).getSecond();
        }
        if( elem instanceof Bindings )
        {
          Bindings elemBindings = (Bindings)elem;
          Bindings properties = getJSchema_Properties( elemBindings );
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
    for( Object elem : list )
    {
      if( elem instanceof Pair )
      {
        elem = ((Pair)elem).getSecond();
      }
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
    String ref = getJSchema_Ref( jsonObj );
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
