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

package manifold.api.json.schema;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.script.Bindings;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.json.DynamicType;
import manifold.api.json.ErrantType;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonListType;
import manifold.api.json.JsonSimpleType;
import manifold.api.json.JsonStructureType;
import manifold.api.json.Token;
import manifold.api.templ.DisableStringLiteralTemplates;
import manifold.internal.javac.IIssue;
import manifold.util.JsonUtil;
import manifold.util.Pair;
import manifold.util.StreamUtil;
import manifold.util.cache.FqnCache;

/**
 */
public class JsonSchemaTransformer
{
  @DisableStringLiteralTemplates
  private static final String JSCH_SCHEMA = "$schema";
  private static final String JSCH_TYPE = "type";
  private static final String JSCH_NAME = "name";
  @DisableStringLiteralTemplates
  private static final String JSCH_ID = "$id";
  @DisableStringLiteralTemplates
  private static final String JSCH_REF = "$ref";
  private static final String JSCH_ENUM = "enum";
  private static final String JSCH_CONST = "const";
  private static final String JSCH_ALL_OF = "allOf";
  private static final String JSCH_ONE_OF = "oneOf";
  private static final String JSCH_ANY_OF = "anyOf";
  static final String JSCH_REQUIRED = "required";
  static final String JSCH_DEFINITIONS = "definitions";
  static final String JSCH_PROPERTIES = "properties";
  private static final String JSCH_FORMAT = "format";
  private static final String JSCH_DEFAULT = "default";

  private final IManifoldHost _host;
  private FqnCache<IJsonType> _typeByFqn;

  private JsonSchemaTransformer( IManifoldHost host )
  {
    _typeByFqn = new FqnCache<>( "doc", true, JsonUtil::makeIdentifier );
    _host = host;
  }

  public static boolean isSchema( Bindings bindings )
  {
           // Ideally the "$schema" element would be required, but JSchema does not require it.
    return bindings.get( JsonSchemaTransformer.JSCH_SCHEMA ) != null ||
           // As a fallback check for "$id" as this is pretty uniquely Json Schema
           bindings.get( JsonSchemaTransformer.JSCH_ID ) != null ||
           // As a fallback to the fallback, check for: "type": "object" or "type": "array"
           typeMatches( bindings, Type.Object ) || typeMatches( bindings, Type.Array );
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
    return typeName.equals( testType.getName() );
  }

  @SuppressWarnings("unused")
  public static IJsonType transform( IManifoldHost host, String name, Bindings docObj )
  {
    return transform( host, name, null, docObj );
  }
  @DisableStringLiteralTemplates
  public static IJsonType transform( IManifoldHost host, String name, URL source, Bindings docObj )
  {
    if( !isSchema( docObj ) )
    {
      ErrantType errant = new ErrantType( source, name );
      errant.addIssue( new JsonIssue( IIssue.Kind.Error, null, "The Json object from '$source' does not contain a '$schema' element." ) );
      return errant;
    }

    JsonSchemaTransformer transformer = new JsonSchemaTransformer( host );
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
      token = ((Token[])((Pair)value).getFirst())[1];
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
      errant.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Malformed URL id: $id" ) );
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

  private static List getJSchema_Const( Bindings docObj )
  {
    Object value = docObj.get( JSCH_CONST );
    List list;
    if( value instanceof Pair )
    {
      list = Collections.singletonList( ((Pair)value ).getSecond() );
    }
    else
    {
      list = Collections.singletonList( value );
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
  private static List getJSchema_AnyOf( Bindings docObj )
  {
    Object value = docObj.get( JSCH_ANY_OF );
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
  private static List getJSchema_OneOf( Bindings docObj )
  {
    Object value = docObj.get( JSCH_ONE_OF );
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
        IJsonType type = transformType( definitionsHolder, enclosing, name, bindings );
        if( tokens != null && type instanceof JsonStructureType )
        {
          ((JsonStructureType)type).setToken( tokens[0] );
        }
        result.add( type );
      }
      catch( Exception e )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, tokens != null ? tokens[1] : null, e.getMessage() ) );
      }
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
    StringBuilder sb = new StringBuilder();
    for( StringTokenizer tokenizer = new StringTokenizer( localRef, "." ); tokenizer.hasMoreTokens(); )
    {
      if( sb.length() > 0 )
      {
        sb.append( '.' );
      }
      String token = tokenizer.nextToken();
      token = JsonUtil.makeIdentifier( token );
      sb.append( token );
    }
    return sb.toString();
  }

  void cacheByFqn( JsonSchemaType type )
  {
    _typeByFqn.add( type.getFqn(), type );
  }
  void cacheSimpleByFqn( JsonSchemaType parent, String name, IJsonType type )
  {
    if( parent instanceof JsonListType )
    {
      return;
    }
    _typeByFqn.add( parent.getFqn() + '.' + name, type );
  }
  private void cacheType( IJsonParentType parent, String name, IJsonType type, Bindings jsonObj )
  {
    Object value = jsonObj.get( JSCH_ID );
    if( value == null )
    {
      if( type instanceof JsonSchemaType )
      {
        cacheByFqn( (JsonSchemaType)type );
      }
      else if( type instanceof LazyRefJsonType )
      {
        cacheSimpleByFqn( (JsonSchemaType)parent, name, type );
      }
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
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Relative 'id' is invalid: empty" ) );
      return;
    }

    String localRef = makeLocalRef( id );
    if( localRef.isEmpty() )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Relative 'id' is invalid: $id" ) );
      return;
    }

    IJsonType existing = findLocalRef( id, null );
    if( existing != null )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Id '" + id + "' already assigned to type '${existing.getName()}'" ) );
    }
    else
    {
      _typeByFqn.add( localRef, type );
    }
  }

  @DisableStringLiteralTemplates
  IJsonType transformType( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    final IJsonType result;
    Object value = jsonObj.get( JSCH_TYPE );
    String type;
    Token token = null;
    if( value instanceof Pair )
    {
      type = (String)((Pair)value).getSecond();
      token = ((Token[])((Pair)value).getFirst())[1];
    }
    else
    {
      type = (String)value;
    }

    if( type == null && isPropertiesDefined( jsonObj ) )
    {
      type = Type.Object.getName();
    }

    Runnable transform = null;

    JsonFormatType formatType = jsonObj.containsKey( JSCH_FORMAT ) ? resolveFormatType( jsonObj ) : null;
    if( formatType != null )
    {
      result = addDefaultValue( jsonObj, formatType );
      cacheSimpleByFqn( parent, name, formatType );
    }
    else
    {
      boolean bRef = jsonObj.containsKey( JSCH_REF );
      boolean bEnum = jsonObj.containsKey( JSCH_ENUM );
      boolean bConst = jsonObj.containsKey( JSCH_CONST );
      if( bEnum )
      {
        result = addDefaultValue( jsonObj, deriveTypeFromEnum( parent, enclosing, name, jsonObj ) );
        if( result != parent )
        {
          transferIssuesFromErrantType( parent, result, jsonObj );
        }
      }
      else if( bConst )
      {
        result = addDefaultValue( jsonObj, deriveTypeFromConst( parent, enclosing, name, jsonObj ) );
        if( result != parent )
        {
          transferIssuesFromErrantType( parent, result, jsonObj );
        }
      }
      else if( type == null || bRef || isCombination( jsonObj ) )
      {
        JsonStructureType refParent = new JsonStructureType( parent, enclosing, name );
        if( bRef && parent == null )
        {
          Object refValue = jsonObj.get( JSCH_REF );
          if( refValue instanceof Pair )
          {
            token = ((Token[])((Pair)refValue).getFirst())[0];
          }
          refParent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "'$ref' not allowed at root level" ) );
          result = refParent;
        }
        else
        {
          transformDefinitions( parent, enclosing, name, jsonObj, refParent );
          IJsonType refOrCombinationType = findReferenceTypeOrCombinationType( parent, enclosing, name, jsonObj );
          result = refOrCombinationType == null ? null : addDefaultValue( jsonObj, refOrCombinationType );
          if( result != parent )
          {
            transferIssuesFromErrantType( parent, result, jsonObj );
          }
        }
      }
      else
      {
        switch( Type.fromName( type ) )
        {
          case Object:
            result = addDefaultValue( jsonObj, new JsonStructureType( parent, enclosing, name ) );
            transform = () -> ObjectTransformer.transform( this, (JsonStructureType)result, jsonObj );
            break;
          case Array:
            result = addDefaultValue( jsonObj, new JsonListType( name, enclosing, parent ) );
            transform = () -> ArrayTransformer.transform( this, name, (JsonListType)result, jsonObj );
            break;
          case String:
            result = addDefaultValue( jsonObj, JsonSimpleType.String );
            cacheSimpleByFqn( parent, name, result );
            break;
          case Number:
            result = addDefaultValue( jsonObj, JsonSimpleType.Double );
            cacheSimpleByFqn( parent, name, result );
            break;
          case Integer:
            result = addDefaultValue( jsonObj, JsonSimpleType.Integer );
            cacheSimpleByFqn( parent, name, result );
            break;
          case Boolean:
            result = addDefaultValue( jsonObj, JsonSimpleType.Boolean );
            cacheSimpleByFqn( parent, name, result );
            break;
          case Null:
            result = JsonSimpleType.Null;
            cacheSimpleByFqn( parent, name, result );
            break;
          case Dynamic:
            result = addDefaultValue( jsonObj, DynamicType.instance() );
            cacheSimpleByFqn( parent, name, result );
            break;
          case Invalid:
          default:
            throw new IllegalSchemaTypeName( type, token );
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

  private IJsonType addDefaultValue( Bindings jsonObj, IJsonType result )
  {
    boolean bDefault = jsonObj.containsKey( JSCH_DEFAULT );
    if( bDefault )
    {
      Object defaultValue = jsonObj.get( JSCH_DEFAULT );
      if( defaultValue instanceof Pair )
      {
        defaultValue = ((Pair)defaultValue).getSecond();
      }
      result = result.setDefaultValue( defaultValue );
    }
    return result;
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
        token = ((Token[])((Pair)value).getFirst())[1];
      }

      for( JsonIssue issue: ((ErrantType)result).getIssues() )
      {
        parent.addIssue( new JsonIssue( issue.getKind(), token, issue.getMessage() ) );
      }
    }
  }

  private IJsonType findReferenceTypeOrCombinationType( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    IJsonType result;
    result = findReference( parent, enclosing, jsonObj );
    if( result == null )
    {
      result = transformCombination( parent, enclosing, name, jsonObj );
      if( result == null )
      {
        result = deriveTypeFromEnum( parent, enclosing, name, jsonObj );
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

  private IJsonType deriveTypeFromEnum( JsonSchemaType parent, URL enclosing, String name, Bindings bindings )
  {
    List<?> list = getJSchema_Enum( bindings );
    return makeEnum( parent, enclosing, name, list );
  }

  private IJsonType deriveTypeFromConst( JsonSchemaType parent, URL enclosing, String name, Bindings bindings )
  {
    // Note the "const" type is shorthand for a single element "enum" type
    List<?> list = getJSchema_Const( bindings );
    return makeEnum( parent, enclosing, name, list );
  }

  private IJsonType makeEnum( JsonSchemaType parent, URL enclosing, String name, List<?> list )
  {
    if( list == null )
    {
      return null;
    }

    JsonEnumType type = new JsonEnumType( parent, enclosing, name, list );
    if( parent != null )
    {
      parent.addChild( type.getLabel(), type );
    }
    return type;
  }

  private IJsonType transformCombination( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    IJsonType type = transformAllOf( parent, enclosing, name, jsonObj );
    if( type != null )
    {
      return type;
    }

    type = transformAnyOf( parent, enclosing, name, jsonObj );
    if( type != null )
    {
      return type;
    }

    return transformOneOf( parent, enclosing, name, jsonObj );
  }

  private JsonStructureType transformAllOf( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    List list = getJSchema_AllOf( jsonObj );
    if( list == null )
    {
      return null;
    }
    return buildHierarchy( parent, enclosing, name, list );
  }

  private IJsonType transformAnyOf( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    List list = getJSchema_AnyOf( jsonObj );
    if( list == null )
    {
      return null;
    }
    return buildUnion( parent, enclosing, name, list );
  }
  private IJsonType transformOneOf( JsonSchemaType parent, URL enclosing, String name, Bindings jsonObj )
  {
    List list = getJSchema_OneOf( jsonObj );
    if( list == null )
    {
      return null;
    }
    return buildUnion( parent, enclosing, name, list );
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
        if( ((Bindings)elem).size() == 1 && ((Bindings)elem).containsKey( JSCH_REQUIRED ) )
        {
          continue;
        }

        Bindings elemBindings = (Bindings)elem;
        IJsonType ref = findReference( parent, enclosing, elemBindings );
        if( ref != null )
        {
          type = type == null ? new JsonStructureType( parent, enclosing, name ) : type;
          type.addSuper( ref );
        }
        else
        {
          Bindings properties = getJSchema_Properties( elemBindings );
          if( properties != null )
          {
            type = type == null ? new JsonStructureType( parent, enclosing, name ) : type;
            ObjectTransformer.transform( this, type, elemBindings );
          }
        }
      }
    }
    return type;
  }

  private IJsonType buildUnion( JsonSchemaType parent, URL enclosing, String name, List list )
  {
    JsonUnionType type = new JsonUnionType( parent, enclosing, name );
    int i = 0;
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
        IJsonType typePart = transformType( type, enclosing, simpleName, (Bindings)elem );
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

  private IJsonType findReference( JsonSchemaType parent, URL enclosing, Bindings jsonObj )
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
      uri = new URI( ref );
    }
    catch( URISyntaxException e )
    {
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI syntax: $ref" ) );
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

      IJsonType definition = pair == null ? null : findFragmentType( enclosing, uri, pair );
      if( definition == null )
      {
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI: $uri" ) );
      }
      return definition;
    }
    else
    {
      // relative to this file

      String fragment = uri.getFragment();
      if( fragment != null )
      {
        return new LazyRefJsonType( () -> {
          IJsonType localRef = findLocalRef( fragment, enclosing );
          if( localRef == null )
          {
            parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Invalid URI fragment: $fragment" ) );
            localRef = new ErrantType( enclosing, fragment );
          }
          return localRef;
        } );
      }
    }

    throw new UnsupportedOperationException( "Unhandled URI: $ref" );
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
      parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Malformed URL: $uri" ) );
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
          IFile file = _host.getFileSystem().getIFile( url );
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
        parent.addIssue( new JsonIssue( IIssue.Kind.Error, token, "Error: ${e.getMessage()}" ) );
        return null;
      }

      String name = new File( uri.getPath() ).getName();
      int iDot = name.lastIndexOf( '.' );
      if( iDot > 0 )
      {
        name = name.substring( 0, iDot );
      }
      baseType = transform( _host, name, url, bindings );
      pair = new Pair<>( baseType, this );
      JsonSchemaTransformerSession.instance().cacheBaseType( url, pair );
    }
    return pair;
  }
}
