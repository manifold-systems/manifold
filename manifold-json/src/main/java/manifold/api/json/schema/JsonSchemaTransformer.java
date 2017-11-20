package manifold.api.json.schema;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import manifold.api.fs.IFile;
import manifold.api.json.DynamicType;
import manifold.api.json.ErrantType;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonListType;
import manifold.api.json.JsonSchemaType;
import manifold.api.json.JsonSimpleType;
import manifold.api.json.JsonStructureType;
import manifold.api.json.Token;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.IIssue;
import manifold.util.JsonUtil;
import manifold.util.Pair;
import manifold.util.StreamUtil;
import manifold.util.cache.FqnCache;

/**
 */
public class JsonSchemaTransformer
{
  private static final String JSCH_SCHEMA = "$schema";
  private static final String JSCH_TYPE = "type";
  private static final String JSCH_NAME = "name";
  private static final String JSCH_ID = "$id";
  private static final String JSCH_REF = "$ref";
  private static final String JSCH_DEFINITIONS = "definitions";
          static final String JSCH_PROPERTIES = "properties";
  private static final String JSCH_ENUM = "enum";
  private static final String JSCH_ALL_OF = "allOf";
  private static final String JSCH_ONE_OF = "oneOf";

  private FqnCache<IJsonType> _typeByFqn;

  private JsonSchemaTransformer()
  {
    _typeByFqn = new FqnCache<>( "doc", true, JsonUtil::makeIdentifier );
  }

  public static boolean isSchema( Bindings bindings )
  {
           // Ideally the "$schema" element would be required, but JSchema does not require it.
    return bindings.get( JsonSchemaTransformer.JSCH_SCHEMA ) != null ||
           // As a fallback check for "$id" as this is pretty uniquely JSchema
           bindings.get( JsonSchemaTransformer.JSCH_ID ) != null;
  }

  @SuppressWarnings("unused")
  public static IJsonType transform( String name, Bindings docObj )
  {
    return transform( name, null, docObj );
  }
  public static IJsonType transform( String name, URL source, Bindings docObj )
  {
    if( !isSchema( docObj ) )
    {
      ErrantType errant = new ErrantType( source, name );
      errant.addIssue( new JsonIssue( IIssue.Kind.Error, null, "The Json object from '" + source + "' does not contain a '$schema' element." ) );
      return errant;
    }

    JsonSchemaTransformer transformer = new JsonSchemaTransformer();
    JsonSchemaTransformerSession session = JsonSchemaTransformerSession.instance();
    session.pushTransformer( transformer );
    try
    {
      name = name == null || name.isEmpty() ? getJSchema_Name( docObj ) : name;
      IJsonType cachedType = findTopLevelCachedType( name, source, docObj );
      if( cachedType != null )
      {
        return cachedType;
      }

      return transformer.transformType( null, source, name, docObj );
    }
    finally
    {
      session.popTransformer( transformer );
    }
  }

  private static IJsonType findTopLevelCachedType( String name, URL source, Bindings docObj )
  {
    if( source != null )
    {
      Pair<IJsonType, JsonSchemaTransformer> pair = JsonSchemaTransformerSession.instance().getCachedBaseType( source );
      if( pair != null )
      {
        return pair.getFirst();
      }
    }

    Object value = docObj.get( JSCH_ID );
    if( value == null )
    {
      return null;
    }

    String id;
    Token token = null;
    if( value instanceof Pair )
    {
      id = (String)((Pair)value).getSecond();
      token = (Token)((Pair)value).getFirst();
    }
    else
    {
      id = (String)value;
    }

    if( id == null || id.isEmpty() )
    {
      return null;
    }

    try
    {
      URL url = new URL( id );
      Pair<IJsonType, JsonSchemaTransformer> pair = JsonSchemaTransformerSession.instance().getCachedBaseType( url );
      if( pair != null )
      {
        return pair.getFirst();
      }
    }
    catch( MalformedURLException e )
    {
      ErrantType errant = new ErrantType( null, name );
      errant.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Malformed URL id: " + id ) );
      return errant;
    }
    return null;
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

  private List<IJsonType> transformDefinitions( JsonSchemaType parent, String nameQualifier, URL enclosing, Bindings jsonObj )
  {
    return transformDefinitions( new JsonStructureType( parent, enclosing, nameQualifier ), enclosing, jsonObj );
  }
  private List<IJsonType> transformDefinitions( JsonSchemaType parent, URL enclosing, Bindings jsonObj )
  {
    Bindings definitions = getJSchema_Definitions( jsonObj );
    if( definitions == null )
    {
      return null;
    }

    JsonStructureType definitionsHolder = new JsonStructureType( parent, enclosing, JSCH_DEFINITIONS );
    List<IJsonType> result = new ArrayList<>();
    cacheByFqn( definitionsHolder );
    for( Map.Entry<String, Object> entry : definitions.entrySet() )
    {
      String name = entry.getKey();
      Object value = entry.getValue();
      Bindings bindings;
      Token token = null;
      if( value instanceof Pair )
      {
        bindings = (Bindings)((Pair)value).getSecond();
        token = (Token)((Pair)value).getFirst();
      }
      else
      {
        bindings = (Bindings)value;
      }
      IJsonType type = transformType( definitionsHolder, enclosing, name, bindings );
      if( token != null && type instanceof JsonStructureType )
      {
        ((JsonStructureType)type).setToken( token );
      }
      result.add( type );
    }
    return result;
  }

  private IJsonType findLocalRef( String localRef, URL enclosing )
  {
    localRef = makeLocalRef( localRef );
    if( localRef.isEmpty() )
    {
      // an empty "#" ref must resolve to the enclosing URL
      Pair<IJsonType, JsonSchemaTransformer> cachedBaseType = JsonSchemaTransformerSession.instance().getCachedBaseType( enclosing );
      if( cachedBaseType != null )
      {
        return cachedBaseType.getFirst();
      }
    }
    return _typeByFqn.get( localRef );
  }

  private String makeLocalRef( String localRef )
  {
    if( localRef.isEmpty() )
    {
      return "";
    }

    localRef = localRef.replace( '/', '.' );
    char firstChar = localRef.charAt( 0 );
    if( firstChar == '.' || firstChar == '#' )
    {
      localRef = localRef.substring( 1 );
    }
    return localRef;
  }

  void cacheByFqn( JsonSchemaType type )
  {
    _typeByFqn.add( makeFqn( type ), type );
  }
  void cacheSimpleByFqn( JsonSchemaType definitionsHolder, String definitionName, IJsonType type )
  {
    _typeByFqn.add( makeFqn( definitionsHolder ) + '.' + definitionName, type );
  }
  private void cacheById( IJsonParentType parent, IJsonType type, Bindings jsonObj )
  {
    Object value = jsonObj.get( JSCH_ID );
    if( value == null )
    {
      return;
    }

    String id;
    Token token = null;
    if( value instanceof Pair )
    {
      id = (String)((Pair)value).getSecond();
      token = (Token)((Pair)value).getFirst();
    }
    else
    {
      id = (String)value;
    }
    cacheById( parent, type, id, token );
  }
  private void cacheById( IJsonParentType parent, IJsonType type, String id, Token token )
  {
    if( id.isEmpty() )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Relative 'id' is invalid: empty" ) );
      return;
    }

    String localRef = makeLocalRef( id );
    if( localRef.isEmpty() )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Relative 'id' is invalid: " + id ) );
      return;
    }

    IJsonType existing = findLocalRef( id, null );
    if( existing != null )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Id '" + id + "' already assigned to type '" + existing.getName() + "'" ) );
    }
    else
    {
      _typeByFqn.add( localRef, type );
    }
  }

  private String makeFqn( JsonSchemaType type )
  {
    String result = "";
    if( !isParentRoot( type ) )
    {
      result = makeFqn( type.getParent() );
      result += '.';
    }
    return result + JsonUtil.makeIdentifier( type.getLabel() );
  }

  private boolean isParentRoot( JsonSchemaType type )
  {
    return type.getParent() == null ||
           type.getParent().getParent() == null && (type.getParent() instanceof JsonListType || !type.getParent().getName().equals( JSCH_DEFINITIONS ));
  }

  IJsonType transformType( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
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

    Runnable transform = null;

    boolean bRef = jsonObj.get( JSCH_REF ) != null;
    if( type == null || bRef )
    {
      JsonStructureType refParent = new JsonStructureType( parent, enclosing, name );
      if( bRef && parent == null )
      {
        Object refValue = jsonObj.get( JSCH_REF );
        if( refValue instanceof Pair )
        {
          token = (Token)((Pair)refValue).getFirst();
        }
        refParent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "'$ref' not allowed at root level" ) );
        result = refParent;
      }
      else
      {
        transformDefinitions( parent, enclosing, name, jsonObj, refParent );
        result = findReferenceType( refParent, enclosing, name, jsonObj );
        transferIssuesFromErrantType( refParent, result, jsonObj );
      }
    }
    else if( jsonObj.get( JSCH_ONE_OF ) != null )
    {
      // "oneOf" is difficult to handle for Java's type system,
      // especially with $ref it basically implies union types, which Java does not support.
      // So for now we punt and deal with it as a dynamic type, which Java does not support,
      // but we will approximate it with Bindings (a map)
      result = DynamicType.instance();
    }
    else
    {
      switch( Type.fromName( type ) )
      {
        case Object:
          result = new JsonStructureType( parent, enclosing, name );
          transform = () -> ObjectTransformer.transform( this, (JsonStructureType)result, jsonObj );
          break;
        case Array:
          result = new JsonListType( name, enclosing, parent );
          transform = () -> ArrayTransformer.transform( this, name, (JsonListType)result, jsonObj );
          break;
        case String:
          result = JsonSimpleType.String;
          cacheSimpleByFqn( parent, name, result );
          break;
        case Number:
          result = JsonSimpleType.Double;
          cacheSimpleByFqn( parent, name, result );
          break;
        case Integer:
          result = JsonSimpleType.Integer;
          cacheSimpleByFqn( parent, name, result );
          break;
        case Boolean:
          result = JsonSimpleType.Boolean;
          cacheSimpleByFqn( parent, name, result );
          break;
        case Dynamic:
        case Null:
          result = DynamicType.instance();
          cacheSimpleByFqn( parent, name, result );
          break;
        case Invalid:
        default:
          throw new IllegalSchemaTypeName( type, token );
      }
      transformDefinitions( parent, enclosing, name, jsonObj, result );
    }


    cacheById( parent, result, jsonObj );
    if( parent == null && enclosing != null )
    {
      JsonSchemaTransformerSession.instance().cacheBaseType( enclosing, new Pair<>( result, this ) );
    }
    if( transform != null )
    {
      transform.run();
    }

    return result;
  }

  private void transformDefinitions( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj, IJsonType result )
  {
    List<IJsonType> definitions;
    if( result instanceof JsonSchemaType )
    {
      definitions = transformDefinitions( (JsonSchemaType)result, enclosing, jsonObj );
    }
    else
    {
      definitions = transformDefinitions( parent, name, enclosing, jsonObj );
    }
    result.setDefinitions( definitions );
  }

  private void transferIssuesFromErrantType( JsonSchemaType parent, IJsonType result, Bindings jsonObj )
  {
    if( result instanceof ErrantType )
    {
      Object value = jsonObj.get( JSCH_REF );
      Token token = null;
      if( value instanceof Pair )
      {
        token = (Token)((Pair)value).getFirst();
      }

      for( JsonIssue issue: ((ErrantType)result).getIssues() )
      {
        parent.addIssue( new JsonIssue( issue.getKind(), token, issue.getMessage() ) );
      }
    }
  }

  private IJsonType findReferenceType( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    IJsonType result;
    result = findReference( parent, enclosing, jsonObj );
    if( result == null )
    {
      result = transformCombination( parent, enclosing, name, jsonObj );
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

  private IJsonType transformCombination( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    List list = getJSchema_AllOf( jsonObj );
    if( list == null )
    {
      return null;
    }

    JsonStructureType type = buildHierarchy( parent, enclosing, name, list );
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

  private JsonStructureType buildHierarchy( JsonSchemaType parent, URL enclosing, String name, List list )
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
        IJsonType ref = findReference( parent, enclosing, elemBindings );
        if( ref != null )
        {
          if( type == null )
          {
            type = new JsonStructureType( parent, enclosing, name );
          }
          type.addSuper( (IJsonParentType)ref );
        }
      }
    }
    return type;
  }

  private IJsonType findReference( JsonSchemaType parent, URL enclosing, Bindings jsonObj )
  {
    Object value = jsonObj.get( JSCH_REF );
    String ref;
    Token token = null;
    if( value instanceof Pair )
    {
      ref = (String)((Pair)value).getSecond();
      token = (Token)((Pair)value).getFirst();
    }
    else
    {
      ref = (String)value;
    }
    if( ref == null )
    {
      return null;
    }

    URI uri;
    try
    {
      uri = new URI( ref );
    }
    catch( URISyntaxException e )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI syntax: " + ref ) );
      return null;
    }

    String filePart = uri.getRawSchemeSpecificPart();
    if( filePart != null && !filePart.isEmpty() )
    {
      Pair<IJsonType, JsonSchemaTransformer> pair = findBaseType( token, parent, enclosing, uri, filePart );

      IJsonType definition = pair == null ? null : findFragmentType( enclosing, uri, pair );
      if( definition == null )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI: " + uri ) );
      }
      return definition;
    }
    else
    {
      // relative to this file

      String fragment = uri.getFragment();
      if( fragment != null )
      {
        IJsonType localRef = findLocalRef( fragment, enclosing );
        if( localRef == null )
        {
          parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI fragment: " + fragment ) );
          localRef = new ErrantType( enclosing, fragment );
        }
        return localRef;
      }
    }

    throw new UnsupportedOperationException( "Unhandled URI: " + ref );
  }

  private IJsonType findFragmentType( URL enclosing, URI uri, Pair<IJsonType, JsonSchemaTransformer> pair )
  {
    String fragment = uri.getFragment();
    IJsonType baseType = pair.getFirst();
    if( fragment == null || fragment.isEmpty() )
    {
      return baseType;
    }

    JsonSchemaTransformer tx = pair.getSecond();
    return tx.findLocalRef( fragment, enclosing );
  }

  private Pair<IJsonType, JsonSchemaTransformer> findBaseType( Token token, JsonSchemaType parent, URL enclosing, URI uri, String filePart )
  {
    URL url;
    String scheme = uri.getScheme();
    try
    {
      if( scheme != null )
      {
        // absolute address
        url = new URL( scheme + ':' + filePart );
      }
      else
      {
        // assume file system relative path
        url = new URL( enclosing, filePart );
      }
    }
    catch( MalformedURLException e )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Malformed URL: " + uri ) );
      return null;
    }

    Pair<IJsonType, JsonSchemaTransformer> pair = JsonSchemaTransformerSession.instance().getCachedBaseType( url );
    IJsonType baseType = pair == null ? null : pair.getFirst();
    if( baseType == null )
    {
      String otherFileContent;
      try
      {
        String protocol = url.getProtocol();
        InputStream input;
        if( protocol != null && protocol.equals( "file" ) )
        {
          // use use IFile if url is a file e.g., IDE file system change caching
          IFile file = ManifoldHost.getFileSystem().getIFile( url );
          input = file.openInputStream();
        }
        else
        {
          input = url.openStream();
        }
        try( InputStream sheeeeit = input )
        {
          otherFileContent = StreamUtil.getContent( new InputStreamReader( sheeeeit ) );
        }
      }
      catch( Exception e )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, e.getMessage() ) );
        return null;
      }

      Bindings bindings;
      try
      {
        bindings = Json.fromJson( otherFileContent );
      }
      catch( Exception e )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Error: " + e.getMessage() ) );
        return null;
      }

      String name = new File( uri.getPath() ).getName();
      int iDot = name.lastIndexOf( '.' );
      if( iDot > 0 )
      {
        name = name.substring( 0, iDot );
      }
      baseType = transform( name, url, bindings );
      pair = new Pair<>( baseType, this );
      JsonSchemaTransformerSession.instance().cacheBaseType( url, pair );
    }
    return pair;
  }
}
