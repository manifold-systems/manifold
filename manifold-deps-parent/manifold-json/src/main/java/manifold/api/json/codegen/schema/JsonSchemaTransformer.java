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

package manifold.api.json.codegen.schema;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.script.Bindings;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.json.codegen.DynamicType;
import manifold.api.json.codegen.ErrantType;
import manifold.api.json.codegen.IJsonParentType;
import manifold.api.json.codegen.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.codegen.JsonBasicType;
import manifold.api.json.JsonIssue;
import manifold.api.json.codegen.JsonListType;
import manifold.api.json.codegen.JsonStructureType;
import manifold.api.json.parser.Token;
import manifold.internal.javac.IIssue;
import manifold.api.util.DebugLogUtil;
import manifold.api.util.ManIdentifierUtil;
import manifold.api.util.Pair;
import manifold.api.util.StreamUtil;
import manifold.api.util.cache.FqnCache;


import static java.nio.charset.StandardCharsets.UTF_8;


/**
 */
public class JsonSchemaTransformer
{
  private static final String JSCH_SCHEMA = "${'$'}schema";
  static final String JSCH_TYPE = "type";
  private static final String JSCH_NAME = "name";
  private static final String JSCH_ID = "${'$'}id";
  private static final String JSCH_ID_OLD = "id";
  private static final String JSCH_REF = "${'$'}ref";
  private static final String JSCH_ENUM = "enum";
  private static final String JSCH_CONST = "const";
  private static final String JSCH_ALL_OF = "allOf";
  private static final String JSCH_ONE_OF = "oneOf";
  private static final String JSCH_ANY_OF = "anyOf";
  static final String JSCH_ITEMS = "items";
  static final String JSCH_REQUIRED = "required";
  public static final String JSCH_DEFINITIONS = "definitions";
  private static final String JSCH_DEFS = "${'$'}defs";
  static final String JSCH_PROPERTIES = "properties";
  private static final String JSCH_FORMAT = "format";

  static final String JSCH_ADDITIONNAL_PROPERTIES = "additionalProperties";
  static final String JSCH_PATTERN_PROPERTIES = "patternProperties";
  static final String JSCH_DEFAULT = "default";
  static final String JSCH_NULLABLE = "nullable";
  static final String JSCH_READONLY = "readOnly";
  static final String JSCH_WRITEONLY = "writeOnly";


  private final IManifoldHost _host;
  private FqnCache<IJsonType> _typeByFqn;
  private Map<String, IJsonType> _typeById;

  private JsonSchemaTransformer( IManifoldHost host )
  {
    _host = host;
    _typeByFqn = new FqnCache<>( "doc", true, ManIdentifierUtil::makeIdentifier );
    _typeById = new HashMap<>();
  }

  public static boolean isSchema( Bindings bindings )
  {
           // Ideally the "$schema" element would be required, but JSON Schema does not require it.
    return bindings.get( JsonSchemaTransformer.JSCH_SCHEMA ) != null ||
           // As a fallback check for "$id" as this is pretty uniquely Json Schema
           bindings.get( JsonSchemaTransformer.JSCH_ID ) != null ||
           // As a fallback to the fallback, check for: "type": "object" or "type": "array"
           typeMatches( bindings, Type.Object ) || typeMatches( bindings, Type.Array ) ||
           // As a fallback to the fallback to the fallback, check for: "properties": (lots of use-cases like this unfortunately)
           bindings.get( JsonSchemaTransformer.JSCH_PROPERTIES ) != null;
  }

  private static boolean typeMatches( Bindings bindings, Type testType )
  {
    Object type = bindings.get( JsonSchemaTransformer.JSCH_TYPE );
    if( type == null )
    {
      return false;
    }
    String typeName;
    if( type instanceof Pair )
    {
      typeName = (String)((Pair)type).getSecond();
    }
    else
    {
      typeName = (String)type;
    }
    return typeName != null && typeName.equals( testType.getName() );
  }

  @SuppressWarnings("unused")
  public static IJsonType transform( IManifoldHost host, String name, Object jsonValue )
  {
    return transform( host, name, null, jsonValue );
  }
  public static IJsonType transform( IManifoldHost host, String name, IFile source, Object jsonValue )
  {
    if( !(jsonValue instanceof Bindings) || !isSchema( (Bindings)jsonValue ) )
    {
      ErrantType errant = new ErrantType( source, name );
      errant.addIssue( new JsonIssue( IIssue.Kind.Error, null, "The Json object from '${'$'}source' does not contain a '${'$'}schema' element." ) );
      return errant;
    }

    JsonSchemaTransformer transformer = new JsonSchemaTransformer( host );
    JsonSchemaTransformerSession session = JsonSchemaTransformerSession.instance();
    session.pushTransformer( transformer );
    try
    {
      Bindings bindings = (Bindings)jsonValue;
      name = name == null || name.isEmpty() ? getJSchema_Name( bindings ) : name;
      IJsonType cachedType = findTopLevelCachedType( name, source, bindings );
      if( cachedType != null && !(cachedType instanceof ErrantType) )
      {
        return cachedType;
      }

      IJsonType type = transformer.transformType( null, source, name, bindings, null );
      if( type instanceof JsonSchemaType )
      {
        checkSynthetic( (JsonSchemaType)type, jsonValue );
        if( cachedType != null )
        {
          // move issues from errant cached type to the actual type
          moveIssuesFromErrantType( (JsonSchemaType)type, (ErrantType)cachedType );
        }
      }
      return type;
    }
    finally
    {
      session.popTransformer( transformer );
    }
  }

  private static void checkSynthetic( JsonSchemaType type, Object jsonObj )
  {
    if( jsonObj instanceof Bindings )
    {
      Object value = ((Bindings)jsonObj).get( "synthetic" );
      if( value instanceof Boolean )
      {
        type.setSyntheticSchema( (Boolean)value );
      }
    }
  }

  private static IJsonType findTopLevelCachedType( String name, IFile source, Bindings docObj )
  {
    if( source != null )
    {
      Pair<IJsonType, JsonSchemaTransformer> pair = JsonSchemaTransformerSession.instance().getCachedBaseType( source );
      if( pair != null )
      {
        return pair.getFirst();
      }
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

  private static Object getJSchema_Id( Bindings docObj )
  {
    Object value = docObj.get( JSCH_ID );
    if( value == null )
    {
      value = docObj.get( JSCH_ID_OLD );
    }
    return value;
  }

  private static Bindings getJSchema_Definitions( Bindings docObj )
  {
    Object value = docObj.get( JSCH_DEFINITIONS );
    if( value == null )
    {
      value = docObj.get( JSCH_DEFS );
    }
    return getBindings( value );
  }

  private static Bindings getBindings( Object value )
  {
    if( value instanceof Pair )
    {
      value = ((Pair)value).getSecond();
    }
    return value instanceof Bindings ? (Bindings)value : null;
  }

  private static Bindings getJSchema_Properties( Bindings docObj )
  {
    Object value = docObj.get( JSCH_PROPERTIES );
    return getBindings( value );
  }

  private static List getJSchema_Enum( Bindings docObj )
  {
    return getList( docObj.get( JSCH_ENUM ) );
  }

  private static List getJSchema_Const( Bindings docObj )
  {
    return getList( docObj.get( JSCH_CONST ) );
  }

  private static List getJSchema_AllOf( Bindings docObj )
  {
    return getList( docObj.get( JSCH_ALL_OF ) );
  }

  private static List getJSchema_AnyOf( Bindings docObj )
  {
    return getList( docObj.get( JSCH_ANY_OF ) );
  }

  private static List getJSchema_OneOf( Bindings docObj )
  {
    return getList( docObj.get( JSCH_ONE_OF ) );
  }

  private static List getList( Object value )
  {
    if( value instanceof Pair )
    {
      value = ((Pair)value).getSecond();
    }
    return value instanceof List ? (List)value : null;
  }

  private List<IJsonType> transformDefinitions( JsonSchemaType parent, String nameQualifier, IFile enclosing, Bindings jsonObj )
  {
    return transformDefinitions( new JsonStructureType( parent, enclosing, nameQualifier, new TypeAttributes() ), enclosing, jsonObj );
  }
  private List<IJsonType> transformDefinitions( JsonSchemaType parent, IFile enclosing, Bindings jsonObj )
  {
    Bindings definitions = getJSchema_Definitions( jsonObj );
    if( definitions == null )
    {
      return null;
    }

    JsonStructureType definitionsHolder = new JsonStructureType( parent, enclosing, JSCH_DEFINITIONS, new TypeAttributes() );
    List<IJsonType> result = new ArrayList<>();
    cacheByFqn( definitionsHolder );
    for( Map.Entry<String, Object> entry : definitions.entrySet() )
    {
      Token[] tokens = null;
      try
      {
        String name = entry.getKey();
        Object value = entry.getValue();
        Bindings bindings;
        if( value instanceof Pair )
        {
          bindings = (Bindings)((Pair)value).getSecond();
          tokens = (Token[])((Pair)value).getFirst();
        }
        else
        {
          bindings = (Bindings)value;
        }
        IJsonType type = transformType( definitionsHolder, enclosing, name, bindings, null );
        if( tokens != null && type instanceof JsonSchemaType )
        {
          ((JsonSchemaType)type).setToken( tokens[0] );
        }
        result.add( type );
      }
      catch( Exception e )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, tokens != null ? tokens[1] : null,
          e.getMessage() == null ? DebugLogUtil.getStackTrace( e ) : e.getMessage() ) );
      }
    }
    return result;
  }

  private IJsonType findLocalRef( String ref, IFile enclosing )
  {
    // Find by ref "id"

    IJsonType type1 = findById( ref );
    if( type1 != null )
    {
      return type1;
    }

    // Find by ref "location path"

    return findByLocationPath( ref, enclosing );
  }

  private IJsonType findByLocationPath( String ref, IFile enclosing )
  {
    String localRef = makeLocalRef( ref );
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

  private IJsonType findById( String ref )
  {
    IJsonType type = _typeById.get( ref );
    if( type != null )
    {
      return type;
    }
    if( !ref.startsWith( "#" ) )
    {
      // If the original ref is prefixed with '#', it was removed by the URI parser, try prepending it and roll dice.
      // (Not sure why the JSON Schema docs uses an example where the 'id' begins with '#', which is the fragment
      // separator char in a URI...)
      type = _typeById.get( '#' + ref );
      if( type != null )
      {
        return type;
      }
    }
    if( ref.startsWith( "/" ) )
    {
      type = _typeById.get( ref.substring( 1 ) );
      if( type != null )
      {
        return type;
      }
    }
    return null;
  }

  private String makeLocalRef( String localRef )
  {
    if( localRef.isEmpty() )
    {
      return "";
    }

    char firstChar = localRef.charAt( 0 );
    if( firstChar == '/' )
    {
      localRef = localRef.substring( 1 );
    }
    StringBuilder sb = new StringBuilder();
    for( StringTokenizer tokenizer = new StringTokenizer( localRef, "/" ); tokenizer.hasMoreTokens(); )
    {
      if( sb.length() > 0 )
      {
        sb.append( '.' );
      }
      String token = tokenizer.nextToken();
      if( sb.length() == 0 )
      {
        // ref looks like: "#someType/foo/bar"
        // thus we first lookup "#someType", and prepend its path
        // {
        //   "definitions": {
        //     "Foo": {
        //       "$id": "#someType"
        //       ...
        //     }
        //   }
        // }
        // "#someType/foo/bar => "#/definitions/foo/bar"
        prependPathToIdRef( token, sb );
        if( sb.length() > 0 )
        {
          continue;
        }
      }

      token = ManIdentifierUtil.makeIdentifier( token );
      sb.append( token );
    }
    return sb.toString();
  }

  private void prependPathToIdRef( String id, StringBuilder sb )
  {
    IJsonType type = findById( id );
    if( type == null )
    {
      // not an id
      return;
    }

    // build a path to the type
    while( type.getParent() != null )
    {
      if( sb.length() > 0 )
      {
        sb.insert( 0, '.' );
      }
      if( type instanceof JsonSchemaType )
      {
        sb.insert( 0, ManIdentifierUtil.makeIdentifier( ((JsonSchemaType)type).getLabel() ) );
      }
      else
      {
        sb.insert( 0, ManIdentifierUtil.makeIdentifier( type.getName() ) );
      }
      type = type.getParent();
    }
  }

  void cacheByFqn( JsonSchemaType type )
  {
    _typeByFqn.add( type.getFqn(), type );
  }
  void cacheSimpleByFqn( JsonSchemaType parent, String name, IJsonType type )
  {
    _typeByFqn.add( parent.getFqn() + '.' + name, type );
  }
  private void cacheType( IJsonParentType parent, String name, IJsonType type, Bindings jsonObj )
  {
    // Cache by FQN

    if( type instanceof JsonSchemaType )
    {
      cacheByFqn( (JsonSchemaType)type );
    }
    else if( type instanceof LazyRefJsonType )
    {
      cacheSimpleByFqn( (JsonSchemaType)parent, name, type );
    }

    // Also cache by Id (if defined)

    Object value = getJSchema_Id( jsonObj );
    if( value == null )
    {
      return;
    }

    String id;
    Token[] tokens = null;
    if( value instanceof Pair )
    {
      id = (String)((Pair)value).getSecond();
      tokens = (Token[])((Pair)value).getFirst();
    }
    else
    {
      id = (String)value;
    }
    cacheTypeById( parent, type, id, tokens != null ? tokens[1] : null );
  }
  private void cacheTypeById( IJsonParentType parent, IJsonType type, String id, Token token )
  {
    if( id.isEmpty() )
    {
      (parent == null ? (IJsonParentType)type : parent)
        .addIssue( new JsonIssue( IIssue.Kind.Error, token, "Relative 'id' is invalid: empty string" ) );
      return;
    }

    IJsonType existing = _typeById.get( id );
    if( existing != null )
    {
      (parent == null ? (IJsonParentType)type : parent)
        .addIssue( new JsonIssue( IIssue.Kind.Error, token, "Id '$id' already assigned to type '${existing.getName()}'" ) );
    }
    else
    {
      _typeById.put( id, type );
    }
  }

  IJsonType transformType( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, Boolean isNullable )
  {
    final IJsonType result;
    Object value = jsonObj.get( JSCH_TYPE );
    TypeResult tr = getTypeFromValue( value );
    String type = (String)tr.type;
    Token token = tr.token;
    Boolean nullable = isNullable( jsonObj, isNullable, tr );

    if( type == null && isPropertiesDefined( jsonObj ) )
    {
      type = Type.Object.getName();
    }

    Runnable transform = null;

    JsonFormatType formatType = jsonObj.containsKey( JSCH_FORMAT ) ? resolveFormatType( jsonObj ) : null;
    if( formatType != null )
    {
      // Copy format type to allow format type services to reuse types
      result = formatType.copyWithAttributes( new TypeAttributes( nullable, jsonObj ) );
      cacheSimpleByFqn( parent, name, result );
    }
    else
    {
      boolean bRef = jsonObj.containsKey( JSCH_REF );
      boolean bEnum = jsonObj.containsKey( JSCH_ENUM );
      boolean bConst = jsonObj.containsKey( JSCH_CONST );
      if( bEnum )
      {
        result = deriveTypeFromEnum( parent, enclosing, name, jsonObj, nullable );
        if( result != parent )
        {
          copyIssuesFromErrantType( parent, result, jsonObj );
        }
      }
      else if( bConst )
      {
        result = deriveTypeFromConst( parent, enclosing, name, jsonObj, nullable );
        if( result != parent )
        {
          copyIssuesFromErrantType( parent, result, jsonObj );
        }
      }
      else if( type == null || bRef || isCombination( jsonObj ) )
      {
        JsonStructureType refParent = new JsonStructureType( parent, enclosing, name, new TypeAttributes() );
        if( bRef && parent == null )
        {
          Object refValue = jsonObj.get( JSCH_REF );
          if( refValue instanceof Pair )
          {
            token = ((Token[])((Pair)refValue).getFirst())[0];
          }
          refParent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "'${'$'}ref' not allowed at root level" ) );
          result = refParent;
        }
        else
        {
          transformDefinitions( parent, enclosing, name, jsonObj, refParent );
          result = findReferenceTypeOrCombinationType( parent, enclosing, name, jsonObj, nullable );
          if( result != parent )
          {
            copyIssuesFromErrantType( parent, result, jsonObj );
          }
          if( result != null )
          {
            // refParent is just a placeholder for definitions until the ref or combo type is constructe.
            // Assign the definitions from the refParent to the actual parent
            List<IJsonType> definitions = refParent.getDefinitions();
            if( definitions != null )
            {
              result.setDefinitions( definitions );
            }
          }
        }
      }
      else
      {
        Type t = Type.fromName( type );
        switch( t )
        {
          case Object:
            result = new JsonStructureType( parent, enclosing, name, new TypeAttributes( nullable, jsonObj ) );
            transform = () -> ObjectTransformer.transform( this, (JsonStructureType)result, jsonObj );
            break;
          case Array:
            result = new JsonListType( name, enclosing, parent, new TypeAttributes( nullable, jsonObj ) );
            transform = () -> ArrayTransformer.transform( this, name, (JsonListType)result, jsonObj );
            break;
          case Dynamic:
            result = DynamicType.instance();
            cacheSimpleByFqn( parent, name, result );
            break;
          case Invalid:
            throw new IllegalSchemaTypeName( type, token );
          default:
            result = new JsonBasicType( t, new TypeAttributes( nullable, jsonObj ) );
            cacheSimpleByFqn( parent, name, result );
            break;
        }
        transformDefinitions( parent, enclosing, name, jsonObj, result );
      }
    }

    cacheType( parent, name, result, jsonObj );
    if( parent == null && enclosing != null )
    {
      JsonSchemaTransformerSession.instance().cacheBaseType( enclosing, new Pair<>( result, this ) );
    }
    if( transform != null )
    {
      transform.run();
    }

    if( result instanceof JsonSchemaType )
    {
      ((JsonSchemaType)result).setJsonSchema();
    }
    return result;
  }

  private Boolean isNullable( Bindings jsonObj, Boolean isNullable, TypeResult tr )
  {
    Boolean nullable = isNullable;
    if( tr.nullable != null )
    {
      nullable = nullable == null ? tr.nullable : nullable || tr.nullable;
    }
    Boolean openApiNullable = isNullable( jsonObj );
    if( openApiNullable != null )
    {
      nullable = nullable == null ? openApiNullable : nullable || openApiNullable;
    }
    return nullable;
  }

  private Boolean isNullable( Bindings jsonObj )
  {
    Object nullable = jsonObj.get( JsonSchemaTransformer.JSCH_NULLABLE );
    if( !(nullable instanceof Boolean) )
    {
      return null;
    }

    return (Boolean)nullable;
  }

  static class TypeResult {Object type; Token token; Boolean nullable; }
  private TypeResult getTypeFromValue( Object value )
  {
    TypeResult tr = new TypeResult();
    if( value instanceof Pair )
    {
      tr.type = ((Pair)value).getSecond();
      tr.token = ((Token[])((Pair)value).getFirst())[1];
    }
    else
    {
      tr.type = value;
    }
    if( tr.type instanceof List )
    {
      //noinspection unchecked
      for( String name: (List<String>)tr.type )
      {
        Type typeName = Type.fromName( name );
        if( typeName == Type.Null )
        {
          tr.nullable = true;
        }
        else
        {
          tr.type = typeName.getName();
        }
      }
    }
    return tr;
  }
  
  private JsonFormatType resolveFormatType( Bindings jsonObj )
  {
    JsonFormatType resolvedType = null;
    for( IJsonFormatTypeResolver resolver: Objects.requireNonNull( FormatTypeResolvers.get() ) )
    {
      Object format = jsonObj.get( JSCH_FORMAT );
      format = format instanceof Pair ? ((Pair)format).getSecond() : format;
      resolvedType = resolver.resolveType( (String)format );
      if( resolvedType != null )
      {
        break;
      }
    }
    return resolvedType;
  }

  private boolean isCombination( Bindings jsonObj )
  {
    return (jsonObj.containsKey( JSCH_ALL_OF ) ||
            jsonObj.containsKey( JSCH_ONE_OF ) ||
            jsonObj.containsKey( JSCH_ANY_OF )) &&
           !isPropertiesDefined( jsonObj );
  }

  private boolean isPropertiesDefined( Bindings jsonObj )
  {
    return (jsonObj.get( JSCH_PROPERTIES ) instanceof Bindings) ||
           jsonObj.get( JSCH_PROPERTIES ) instanceof Pair && ((Pair)jsonObj.get( JSCH_PROPERTIES )).getSecond() instanceof Bindings;
  }

  private void transformDefinitions( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, IJsonType result )
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

  private static void copyIssuesFromErrantType( JsonSchemaType parent, IJsonType type, Bindings jsonObj )
  {
    if( type instanceof ErrantType )
    {
      Object value = jsonObj.get( JSCH_REF );
      Token token = null;
      if( value instanceof Pair )
      {
        token = ((Token[])((Pair)value).getFirst())[1];
      }

      for( JsonIssue issue: ((ErrantType)type).getIssues() )
      {
        parent.addIssue( new JsonIssue( issue.getKind(), token, issue.getMessage() ) );
      }
    }
  }

  private static void moveIssuesFromErrantType( JsonSchemaType target, ErrantType errant )
  {
    for( JsonIssue issue: errant.getIssues() )
    {
      target.addIssue( issue );
    }
  }

  private IJsonType findReferenceTypeOrCombinationType( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, Boolean nullable )
  {
    IJsonType result;
    result = findReference( parent, enclosing, jsonObj );
    if( result != null )
    {
      result = result.copyWithAttributes( new TypeAttributes( nullable, jsonObj ) );
    }
    else
    {
      result = transformCombination( parent, enclosing, name, jsonObj, nullable );
      if( result == null )
      {
        result = deriveTypeFromEnum( parent, enclosing, name, jsonObj, nullable );
        if( result == null )
        {
          // No type or other means of deriving a type could be found.
          // Default type is Dynamic (in Java this is Object)
          result = DynamicType.instance();
        }
      }
    }
    return result;
  }

  private IJsonType deriveTypeFromEnum( JsonSchemaType parent, IFile enclosing, String name, Bindings bindings, Boolean nullable )
  {
    List<?> list = getJSchema_Enum( bindings );
    return makeEnum( parent, enclosing, name, list, new TypeAttributes( nullable, bindings ) );
  }

  private IJsonType deriveTypeFromConst( JsonSchemaType parent, IFile enclosing, String name, Bindings bindings, Boolean nullable )
  {
    // Note the "const" type is shorthand for a single element "enum" type
    List<?> list = getJSchema_Const( bindings );
    return makeEnum( parent, enclosing, name, list, new TypeAttributes( nullable, bindings ) );
  }

  private IJsonType makeEnum( JsonSchemaType parent, IFile enclosing, String name, List<?> list, TypeAttributes attr )
  {
    if( list == null )
    {
      return null;
    }

    JsonEnumType type = new JsonEnumType( parent, enclosing, name, list, attr );
    if( parent != null )
    {
      parent.addChild( type.getLabel(), type );
    }
    return type;
  }

  private IJsonType transformCombination( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, Boolean nullable )
  {
    IJsonType type = transformAllOf( parent, enclosing, name, jsonObj, nullable );
    if( type != null )
    {
      return type;
    }

    type = transformAnyOf( parent, enclosing, name, jsonObj, nullable );
    if( type != null )
    {
      return type;
    }

    return transformOneOf( parent, enclosing, name, jsonObj, nullable );
  }

  private JsonStructureType transformAllOf( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, Boolean nullable )
  {
    List list = getJSchema_AllOf( jsonObj );
    if( list == null )
    {
      return null;
    }
    return buildHierarchy( parent, enclosing, name, list, jsonObj, nullable );
  }

  private IJsonType transformAnyOf( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, Boolean nullable )
  {
    List list = getJSchema_AnyOf( jsonObj );
    if( list == null )
    {
      return null;
    }
    return buildUnion( parent, enclosing, name, list, jsonObj, nullable );
  }
  private IJsonType transformOneOf( JsonSchemaType parent, IFile enclosing, String name, Bindings jsonObj, Boolean nullable )
  {
    List list = getJSchema_OneOf( jsonObj );
    if( list == null )
    {
      return null;
    }
    return buildUnion( parent, enclosing, name, list, jsonObj, nullable );
  }

  private JsonStructureType buildHierarchy( JsonSchemaType parent, IFile enclosing, String name, List list, Bindings jsonObj, Boolean nullable )
  {
    JsonStructureType type = null;
    boolean hasType = false;
    int iInner = 0;
    for( Object elem : list )
    {
      if( elem instanceof Pair )
      {
        elem = ((Pair)elem).getSecond();
      }

      if( elem instanceof Bindings )
      {
        Bindings elemBindings = (Bindings)elem;

        type = type == null ? new JsonStructureType( parent, enclosing, name, new TypeAttributes( nullable, jsonObj ) ) : type;

        if( elemBindings.size() == 1 && elemBindings.containsKey( JSCH_REQUIRED ) )
        {
          //
          // "required"
          //
          
          Object requiredValue = elemBindings.get( JsonSchemaTransformer.JSCH_REQUIRED );
          type.addRequiredWithTokens( requiredValue );
        }

        IJsonType ref = findReference( type, enclosing, elemBindings );
        if( ref != null )
        {
          //
          // "$ref"
          //

          if( !hasType )
          {
            ObjectTransformer.transform( this, type, elemBindings );
            hasType = true;
          }
          type.addSuper( ref );
        }
        else if( elemBindings.containsKey( JSCH_ENUM ) )
        {
          //
          // "enum"
          //

          if( !hasType )
          {
            ObjectTransformer.transform( this, type, elemBindings );
            hasType = true;
          }

          IJsonType enumType = deriveTypeFromEnum( type, enclosing, "enum" + iInner++, elemBindings, nullable );
          if( enumType != parent )
          {
            copyIssuesFromErrantType( parent, enumType, elemBindings );
          }
          // Note enunType can't really be a super type. Basically if any types in an "allOf" are enum, they all have
          // to be, thus the code gen logic can turn this structure type into a single enum type collapsing all enums
          // into one. This includes any $ref enum types that may be super types.
          type.addSuper( enumType );
        }
        else
        {
          Bindings properties = getJSchema_Properties( elemBindings );
          if( properties != null )
          {
            //
            // "properties"
            //

            ObjectTransformer.transform( this, type, elemBindings );
            hasType = true;
            type = (JsonStructureType)type.copyWithAttributes( new TypeAttributes( elemBindings ) );
          }
          else
          {
            //
            // allOf", "oneOf", "anyOf"
            //

            IJsonType comboType = transformCombination( type, enclosing, "Combo" + iInner++, elemBindings, nullable );
            if( comboType != parent )
            {
              copyIssuesFromErrantType( parent, comboType, elemBindings );
              // special handling required for combo as super type
              type.addSuper( comboType );
            }
          }
        }
      }
    }
    return hasType ? type : null;
  }

  private IJsonType buildUnion( JsonSchemaType parent, IFile enclosing, String name, List list, Bindings jsonObj, Boolean nullable )
  {
    IJsonType singleNullable = maybeGetSingleNullable( parent, enclosing, name, list );
    if( singleNullable != null )
    {
      return singleNullable;
    }

    JsonUnionType type = new JsonUnionType( parent, enclosing, name, new TypeAttributes( nullable, jsonObj ) );
    int i = 0;
    Boolean isNullable = isNullable( list );
    nullable = nullable == null
               ? isNullable
               : isNullable == null
                 ? nullable
                 : (Boolean)(isNullable || nullable);
    for( Object elem : list )
    {
      if( elem instanceof Pair )
      {
        elem = ((Pair)elem).getSecond();
      }

      if( elem instanceof Bindings )
      {
        if( ((Bindings)elem).size() == 1 && ((Bindings)elem).containsKey( JSCH_REQUIRED ) )
        {
          continue;
        }

        String simpleName = "Option" + (i++);
        IJsonType typePart = transformType( type, enclosing, simpleName, (Bindings)elem, nullable );
        String actualName = typePart == null
                            ? null
                            : typePart instanceof LazyRefJsonType
                              ? "Lazy" + System.identityHashCode( typePart )
                              : typePart.getName();
        if( actualName == null || !actualName.equals( simpleName ) )
        {
          i--;
        }
        if( typePart != null )
        {
          type.addConstituent( actualName, typePart );
        }
      }
    }
    if( !type.getConstituents().isEmpty() )
    {
      if( parent != null )
      {
        parent.addChild( type.getLabel(), type );
      }
      return type;
    }
    return null;
  }

  Boolean isNullable( List list )
  {
    for( Object elem: list )
    {
      if( elem instanceof Pair )
      {
        elem = ((Pair)elem).getSecond();
      }

      if( "null".equals( ((Bindings)elem).get( JSCH_TYPE ) ) )
      {
        return true;
      }
    }
    return null;
  }

  private IJsonType maybeGetSingleNullable( JsonSchemaType parent, IFile enclosing, String name, List list )
  {
    if( list.size() != 2 )
    {
      return null;
    }
    Object first = list.get( 0 );
    if( first instanceof Pair )
    {
      first = ((Pair)first).getSecond();
    }
    Object second = list.get( 1 );
    if( second instanceof Pair )
    {
      second = ((Pair)second).getSecond();
    }
    if( first instanceof Bindings )
    {
      Object type = ((Bindings)first).get( JSCH_TYPE );
      if( type instanceof Pair )
      {
        type = ((Pair)type).getSecond();
      }
      boolean nullable = "null".equals( type );
      if( !nullable )
      {
        if( second instanceof Bindings )
        {
          type = ((Bindings)second).get( JSCH_TYPE );
          if( type instanceof Pair )
          {
            type = ((Pair)type).getSecond();
          }

          nullable = "null".equals( type );
          if( nullable )
          {
            return transformType( parent, enclosing, name, (Bindings)first, true );
          }
        }
      }
      else
      {
        return transformType( parent, enclosing, name, (Bindings)second, true );
      }
    }
    return null;
  }

  private IJsonType findReference( JsonSchemaType parent, IFile enclosing, Bindings jsonObj )
  {
    Object value = jsonObj.get( JSCH_REF );
    String ref;
    Token token;
    if( value instanceof Pair )
    {
      ref = (String)((Pair)value).getSecond();
      token = ((Token[])((Pair)value).getFirst())[1];
    }
    else
    {
      ref = (String)value;
      token = null;
    }

    if( ref == null )
    {
      return null;
    }

    URI uri;
    try
    {
      // support refs like: "#/properties/blah".  They are the same as: "#/blah"
      ref = ref.replace( "/properties/", "/" ).replace( "/properties#", "/" );
      uri = new URI( ref );
    }
    catch( URISyntaxException e )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI syntax: ${e.getReason()}" ) );
      return null;
    }

    String scheme = uri.getScheme();
    if( scheme != null && !scheme.isEmpty() && !scheme.equalsIgnoreCase( "file" ) )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Unsupported URI scheme: '$scheme'. A reference must be local to a resource file." ) );
      return null;
    }

    String filePart = uri.getRawSchemeSpecificPart();
    if( filePart != null && !filePart.isEmpty() )
    {
      Pair<IJsonType, JsonSchemaTransformer> pair = findBaseType( token, parent, enclosing, uri, filePart );

      IJsonType definition = pair == null ? null : findFragmentType( token, uri, pair );
      if( definition == null )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI: $uri" ) );
      }
      return definition;
    }
    else
    {
      // relative to this file

      IJsonType fragment = findFragmentRef( parent, enclosing, token, uri );
      if( fragment != null )
      {
        return fragment;
      }
    }

    throw new UnsupportedOperationException( "Unhandled URI: $ref" );
  }

  private IJsonType findFragmentRef( JsonSchemaType parent, IFile enclosing, Token token, URI uri )
  {
    String uriFragment = uri.getFragment();
    if( uriFragment != null )
    {
      String fragment = uriFragment.replace( JSCH_DEFS, JSCH_DEFINITIONS );
      return new LazyRefJsonType( () -> {
        IJsonType localRef = findLocalRef( fragment, enclosing );
        if( localRef == null )
        {
          parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Cannot resolve reference: $fragment" ) );
          localRef = new ErrantType( enclosing, fragment );
        }
        return localRef;
      } );
    }
    return null;
  }

  private IJsonType findFragmentType( Token token, URI uri, Pair<IJsonType, JsonSchemaTransformer> pair )
  {
    String fragment = uri.getFragment();
    IJsonType baseType = pair.getFirst();
    if( fragment == null || fragment.isEmpty() )
    {
      return baseType;
    }

    return pair.getSecond().findFragmentRef( (JsonSchemaType)baseType, ((JsonSchemaType)baseType).getFile(), token, uri );
  }

  private Pair<IJsonType, JsonSchemaTransformer> findBaseType( Token token, JsonSchemaType parent, IFile enclosing, URI uri, String filePart )
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
        url = new URL( enclosing.toURI().toURL(), filePart );
      }
    }
    catch( MalformedURLException e )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Malformed URL: $uri" ) );
      return null;
    }

    IFile urlFile = _host.getFileSystem().getIFile( url );
    Pair<IJsonType, JsonSchemaTransformer> pair = JsonSchemaTransformerSession.instance()
      .getCachedBaseType( urlFile );
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
          input = urlFile.openInputStream();
        }
        else
        {
          input = url.openStream();
        }
        try( InputStream sheeeeit = input )
        {
          otherFileContent = StreamUtil.getContent( new InputStreamReader( sheeeeit, UTF_8 ) );
        }
      }
      catch( Exception e )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, e.getMessage() ) );
        return null;
      }

      Object jsonObject;
      try
      {
        jsonObject = Json.fromJson( otherFileContent );
      }
      catch( Exception e )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Error: ${e.getMessage()}" ) );
        return null;
      }

      String name = new File( uri.getPath() ).getName();
      int iDot = name.lastIndexOf( '.' );
      if( iDot > 0 )
      {
        name = name.substring( 0, iDot );
      }
      transform( _host, name, urlFile, jsonObject );
      pair = JsonSchemaTransformerSession.instance().getCachedBaseType( urlFile );
    }
    return pair;
  }
}
